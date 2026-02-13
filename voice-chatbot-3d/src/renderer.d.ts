export interface IElectronAPI {
  executeScript: (command: string) => void;
  invokeAI: (prompt: string) => Promise<string>;
}

declare global {
  interface Window {
    electron: IElectronAPI;
  }
}
