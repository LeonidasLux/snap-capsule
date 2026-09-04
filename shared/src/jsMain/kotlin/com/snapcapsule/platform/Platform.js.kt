package com.snapcapsule.platform

import com.snapcapsule.ui.CapBridge
import com.snapcapsule.ui.ImportDispatcher

actual fun installPlatformHandlers() {
    // 导出：浏览器下载 JSON 文件
    CapBridge.onExportJson = { fileName, text ->
        val download: dynamic = js(
            """(function (text, fileName) {
                  var blob = new Blob([text], {type: 'application/json'});
                  var url = URL.createObjectURL(blob);
                  var a = document.createElement('a');
                  a.href = url;
                  a.download = fileName;
                  document.body.appendChild(a);
                  a.click();
                  setTimeout(function () { a.remove(); URL.revokeObjectURL(url); }, 200);
              })"""
        )
        download(text, fileName)
    }

    // 导入：隐藏 <input type=file> + FileReader，内容交 ImportDispatcher 由 UI 统一消费
    CapBridge.onPickImportFile = {
        val input: dynamic = js(
            """(function () {
                  var i = document.createElement('input');
                  i.type = 'file';
                  i.accept = '.json,application/json';
                  return i;
              })()"""
        )
        input.onchange = { evt: dynamic ->
            val file: dynamic = evt.target.files[0]
            val reader: dynamic = js("new FileReader()")
            reader.onload = { _: dynamic ->
                val content: String = (reader.result as String?) ?: ""
                ImportDispatcher.pendingText = content
            }
            reader.readAsText(file)
        }
        input.click()
    }
}
