#!/usr/bin/env node
/**
 * 依赖为零的压测脚本（只用 Node 内置模块，Windows 上也能跑）。
 *
 *   node tools/loadtest.js --url http://localhost:8080/front/books/home \
 *        --concurrency 50 --requests 5000
 *
 * 参数：
 *   --url          目标地址（必填）
 *   --concurrency  并发数，默认 50
 *   --requests     总请求数，默认 2000
 *   --timeout      单请求超时毫秒，默认 10000
 *   --header       额外请求头，形如 "Authorization: Bearer xxx"，可重复
 *
 * 输出吞吐、p50/p90/p99、失败数，以及非 2xx 的状态码分布。
 * 注意：HTTP 200 + body 里 code != "00000" 也算业务失败，脚本会一并统计，
 * 因为本项目的全局异常处理器会把部分错误包成 200。
 */
const http = require('node:http')
const https = require('node:https')
const { URL } = require('node:url')

function parseArgs(argv) {
  const args = { concurrency: 50, requests: 2000, timeout: 10000, header: [] }
  for (let i = 2; i < argv.length; i += 1) {
    const key = argv[i]
    const value = argv[i + 1]
    if (key === '--url') { args.url = value; i += 1 }
    else if (key === '--concurrency') { args.concurrency = Number(value); i += 1 }
    else if (key === '--requests') { args.requests = Number(value); i += 1 }
    else if (key === '--timeout') { args.timeout = Number(value); i += 1 }
    else if (key === '--header') { args.header.push(value); i += 1 }
  }
  if (!args.url) {
    console.error('缺少 --url')
    process.exit(1)
  }
  return args
}

function buildHeaders(raw) {
  const headers = { 'User-Agent': 'loadtest' }
  for (const item of raw) {
    const idx = item.indexOf(':')
    if (idx > 0) headers[item.slice(0, idx).trim()] = item.slice(idx + 1).trim()
  }
  return headers
}

const args = parseArgs(process.argv)
const target = new URL(args.url)
const client = target.protocol === 'https:' ? https : http
const agent = new client.Agent({ keepAlive: true, maxSockets: args.concurrency })
const headers = buildHeaders(args.header)

const statusBuckets = new Map()
const latencies = []
let businessFailures = 0
let networkFailures = 0
let next = 0

function once() {
  return new Promise((resolve) => {
    const startedAt = process.hrtime.bigint()
    const req = client.request(
      { hostname: target.hostname, port: target.port, path: target.pathname + target.search,
        method: 'GET', headers, agent },
      (res) => {
        let body = ''
        res.setEncoding('utf8')
        res.on('data', (chunk) => { body += chunk })
        res.on('end', () => {
          const costMs = Number(process.hrtime.bigint() - startedAt) / 1e6
          latencies.push(costMs)
          statusBuckets.set(res.statusCode, (statusBuckets.get(res.statusCode) || 0) + 1)
          if (res.statusCode >= 400) {
            networkFailures += 1
          } else if (body && !body.includes('"code":"00000"') && !body.includes('"status":"UP"')) {
            businessFailures += 1
          }
          resolve()
        })
      })
    req.setTimeout(args.timeout, () => { req.destroy(new Error('timeout')) })
    req.on('error', () => { networkFailures += 1; resolve() })
    req.end()
  })
}

async function worker() {
  while (true) {
    const index = next++
    if (index >= args.requests) return
    await once()
  }
}

function percentile(sorted, p) {
  if (sorted.length === 0) return 0
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return sorted[Math.max(0, idx)]
}

async function main() {
  const startedAt = Date.now()
  await Promise.all(Array.from({ length: args.concurrency }, worker))
  const elapsedSec = (Date.now() - startedAt) / 1000
  const sorted = [...latencies].sort((a, b) => a - b)
  const round = (n) => Math.round(n * 10) / 10

  console.log(`目标        ${args.url}`)
  console.log(`并发/总数   ${args.concurrency} / ${args.requests}`)
  console.log(`耗时        ${round(elapsedSec)} s`)
  console.log(`吞吐        ${round(latencies.length / elapsedSec)} req/s`)
  console.log(`p50 / p90 / p99   ${round(percentile(sorted, 50))} / ${round(percentile(sorted, 90))} / ${round(percentile(sorted, 99))} ms`)
  console.log(`最慢        ${round(sorted[sorted.length - 1] || 0)} ms`)
  console.log(`HTTP 失败   ${networkFailures}`)
  console.log(`业务失败    ${businessFailures}`)
  console.log(`状态码      ${[...statusBuckets.entries()].map(([k, v]) => `${k}×${v}`).join('  ') || '-'}`)
}

main()
