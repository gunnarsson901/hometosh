const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electron', {
  executeScript: (command) => ipcRenderer.send('execute-script', command),
  invokeAI: (prompt) => ipcRenderer.invoke('ai-request', prompt),
});
