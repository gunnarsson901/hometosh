import React, { useState, useEffect } from 'react';
import scripts from '../config/scripts.json';
import personality from '../config/personality.json';

const VoiceController = ({ onSpeakingChange, onTranscriptChange, onResponseChange }) => {
  const [isListening, setIsListening] = useState(true);

  const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();

  recognition.continuous = true;
  recognition.interimResults = false;
  recognition.lang = 'en-US';

  const speak = (text) => {
    onResponseChange(text);
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.onstart = () => onSpeakingChange(true);
    utterance.onend = () => {
      onSpeakingChange(false);
      onResponseChange(''); // Clear response after speaking
    };
    speechSynthesis.speak(utterance);
  };

  const handleCommand = async (text) => {
    const lowerCaseText = text.toLowerCase();
    for (const script of scripts.scripts) {
      for (const phrase of script.phrases) {
        if (lowerCaseText.includes(phrase)) {
          speak(`Executing ${script.name}`);
          window.electron.executeScript(script.command);
          return;
        }
      }
    }
    // If no command is found, send to AI
    const aiResponse = await window.electron.invokeAI(text);
    speak(aiResponse);
  };

  useEffect(() => {
    speak(personality.introduction);
  }, []);

  useEffect(() => {
    recognition.onresult = (event) => {
      const finalTranscript = event.results[event.results.length - 1][0].transcript.trim();
      if (finalTranscript) {
        onTranscriptChange(finalTranscript);
        handleCommand(finalTranscript);
      }
    };

    recognition.onerror = (event) => {
      console.error('Speech recognition error:', event.error);
    };

    recognition.onend = () => {
      if (isListening) {
        recognition.start();
      }
      onTranscriptChange(''); // Clear transcript when listening stops/restarts
    };

    recognition.start();

    return () => {
      recognition.stop();
    };
  }, [isListening]);

  return null;
};

export default VoiceController;
