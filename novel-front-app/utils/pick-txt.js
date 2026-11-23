export function pickTxt() {
  // #ifdef H5
  return new Promise((resolve, reject) => {
    uni.chooseFile({ count: 1, extension: ['txt'],
      success: ({ tempFiles, tempFilePaths }) => {
        const file = tempFiles?.[0]
        if (!file) return reject(new Error('未选择文件'))
        resolve({ path: tempFilePaths?.[0] || file.path, name: file.name, size: file.size })
      },
      fail: (error) => reject(new Error(error.errMsg || '文件选择失败')),
    })
  })
  // #endif
  // #ifdef APP-PLUS
  if (plus.os.name !== 'Android') return Promise.reject(new Error('当前平台尚未支持 TXT 文件选择'))
  return new Promise((resolve, reject) => {
    const activity = plus.android.runtimeMainActivity()
    const Intent = plus.android.importClass('android.content.Intent')
    const FileInputStream = plus.android.importClass('java.io.FileInputStream')
    const FileOutputStream = plus.android.importClass('java.io.FileOutputStream')
    const File = plus.android.importClass('java.io.File')
    const intent = new Intent('android.intent.action.OPEN_DOCUMENT')
    intent.setType('text/plain')
    intent.addCategory('android.intent.category.OPENABLE')
    const requestCode = 3027
    const previousHandler = activity.onActivityResult
    activity.onActivityResult = (code, resultCode, result) => {
      if (code !== requestCode) {
        if (typeof previousHandler === 'function') previousHandler(code, resultCode, result)
        return
      }
      activity.onActivityResult = previousHandler
      if (resultCode !== -1 || !result) return reject(new Error('已取消选择'))
      let descriptor, input, output, cursor, file
      try {
        const uri = result.getData()
        const resolver = activity.getContentResolver()
        cursor = resolver.query(uri, null, null, null, null)
        let name = '导入小说.txt'
        if (cursor && cursor.moveToFirst()) {
          const index = cursor.getColumnIndex('_display_name')
          if (index >= 0) name = String(cursor.getString(index))
        }
        if (!name.toLowerCase().endsWith('.txt')) throw new Error('请选择 TXT 文件')
        descriptor = resolver.openFileDescriptor(uri, 'r')
        const size = Number(descriptor.getStatSize())
        if (size === 0 || size > 20 * 1024 * 1024) throw new Error('文件须非空且不超过 20 MB')
        const path = `${activity.getCacheDir().getAbsolutePath()}/novel-import-${Date.now()}.txt`
        file = new File(path)
        input = new FileInputStream(descriptor.getFileDescriptor())
        output = new FileOutputStream(file)
        const source = input.getChannel(), target = output.getChannel()
        let copied = 0
        while (copied <= 20 * 1024 * 1024) {
          const count = Number(target.transferFrom(source, copied, 1024 * 1024))
          if (!count) break
          copied += count
        }
        if (!copied || copied > 20 * 1024 * 1024) throw new Error('文件须非空且不超过 20 MB')
        resolve({ path, name, size: copied })
      } catch (error) { if (file) file.delete(); reject(new Error(error.message || '读取 TXT 文件失败')) }
      finally {
        if (cursor) cursor.close()
        if (input) input.close()
        if (output) output.close()
        if (descriptor) descriptor.close()
      }
    }
    try { activity.startActivityForResult(intent, requestCode) }
    catch (error) { activity.onActivityResult = previousHandler; reject(new Error(error.message || '无法打开文件选择器')) }
  })
  // #endif
}
