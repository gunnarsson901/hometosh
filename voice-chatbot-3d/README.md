# Voice Controlled 3D Chatbot

This is a voice-controlled 3D chatbot application built with Electron, React, Three.js, and TypeScript.

## How to run the application

1.  Install dependencies: `npm install`
2.  Start the application: `npm start`

## How to replace the 3D model

The application uses a placeholder cube as the 3D model. You can replace it with your own model by following these steps:

1.  **Convert your model to the glTF format.**
    The application uses the `glTF` format for 3D models. If your model is in a different format (e.g., `FBX`, `OBJ`), you can convert it to `glTF` using a tool like Blender.

    *   [Download Blender](https://www.blender.org/download/)
    *   Open Blender and import your model (`File > Import`).
    *   Export the model as a `glTF` file (`File > Export > glTF 2.0`). Make sure to select the "glTF Binary (.glb)" format.

2.  **Add the model to the project.**
    Create a `public` directory in the root of the project and place your `.glb` file inside it.

3.  **Update the code to load the new model.**
    You will need to modify the `renderer.tsx` file to load your new model.

    *   First, install the `gltf-pipeline` to load the model: `npm install --save-dev gltf-pipeline`
    *   Then, update the `AnimatedCube` component in `renderer.tsx` to use `useGLTF` from `@react-three/drei` to load your model.

    Here is an example of how to load a model named `mage.glb`:

    ```tsx
    import React, { useRef, useState } from 'react';
    import { createRoot } from 'react-dom/client';
    import { Canvas, useFrame } from '@react-three/fiber';
    import { useGLTF } from '@react-three/drei';
    import VoiceController from './VoiceController';
    import './style.css';

    const Model = ({ isSpeaking }) => {
      const { scene } = useGLTF('/mage.glb');
      const ref = useRef();

      useFrame((state, delta) => {
        if (ref.current) {
          if (isSpeaking) {
            const scale = 1 + Math.sin(state.clock.elapsedTime * 5) * 0.2;
            ref.current.scale.set(scale, scale, scale);
          } else {
            ref.current.scale.set(1, 1, 1);
          }
        }
      });

      return <primitive object={scene} ref={ref} />;
    }

    const App = () => {
      const [isSpeaking, setIsSpeaking] = useState(false);
      const isSpeechRecognitionSupported = 'SpeechRecognition' in window || 'webkitSpeechRecognition' in window;

      return (
        <>
          <Canvas>
            <ambientLight />
            <pointLight position={[10, 10, 10]} />
            <Model isSpeaking={isSpeaking} />
          </Canvas>
          {isSpeechRecognitionSupported ? (
            <VoiceController onSpeakingChange={setIsSpeaking} />
          ) : (
            <h1 style={{ position: 'absolute', top: 20, left: 20, color: 'white' }}>
              Speech recognition not supported
            </h1>
          )}
        </>
      );
    };

    const root = createRoot(document.getElementById('root'));
    root.render(<App />);
    ```
