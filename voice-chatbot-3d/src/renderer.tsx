import React, { useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Canvas, useFrame } from '@react-three/fiber';
import { useGLTF } from '@react-three/drei';
import VoiceController from './VoiceController';
import './style.css';

const DialogBox = ({ transcript, response }) => {
  return (
    <div
      style={{
        position: 'absolute',
        bottom: '5%',
        left: '50%',
        transform: 'translateX(-50%)',
        width: '80%',
        maxWidth: '800px',
        padding: '20px',
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        color: 'white',
        borderRadius: '10px',
        textAlign: 'center',
        fontFamily: 'monospace',
        fontSize: '1.2rem',
        minHeight: '100px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
      }}
    >
      <div style={{ opacity: 0.7, marginBottom: '10px' }}>
        <span style={{ fontWeight: 'bold' }}>Heard:</span> {transcript}
      </div>
      <div style={{ fontWeight: 'bold', fontSize: '1.5rem', minHeight: '40px' }}>
        {response}
      </div>
    </div>
  );
};

const Model = ({ isSpeaking }) => {
  const { scene } = useGLTF('/characters/Blank_Mask_g.glb');
  const ref = useRef();

  useFrame((state, delta) => {
    if (ref.current) {
      if (isSpeaking) {
        const scale = 2 + Math.sin(state.clock.elapsedTime * 5) * 0.2;
        ref.current.scale.set(scale, scale, scale);
      } else {
        ref.current.scale.set(2, 2, 2);
      }
      ref.current.rotation.y += 0.005;
    }
  });

  return <primitive object={scene} ref={ref} position={[0, 1.0, 0]} />;
}

const App = () => {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [response, setResponse] = useState('');
  const isSpeechRecognitionSupported = 'SpeechRecognition' in window || 'webkitSpeechRecognition' in window;

  return (
    <>
      <Canvas>
        <ambientLight intensity={0.5} />
        <pointLight position={[10, 10, 10]} intensity={1} />
        <spotLight position={[-10, 10, -10]} angle={0.15} penumbra={1} intensity={1} />
        <Model isSpeaking={isSpeaking} />
      </Canvas>
      {isSpeechRecognitionSupported ? (
        <VoiceController
          onSpeakingChange={setIsSpeaking}
          onTranscriptChange={setTranscript}
          onResponseChange={setResponse}
        />
      ) : (
        <h1 style={{ position: 'absolute', top: 20, left: 20, color: 'white' }}>
          Speech recognition not supported
        </h1>
      )}
      <DialogBox transcript={transcript} response={response} />
    </>
  );
};

const root = createRoot(document.getElementById('root'));
root.render(<App />);
