import * as React from 'react'
import { View, Text, Pressable, Platform, PermissionsAndroid, Alert } from 'react-native'
import { QRScannerView } from '../dist'

export default function App() {
  const [screen, setScreen] = React.useState<'home' | 'camera' | 'result'>('home')
  const [qr, setQr] = React.useState<string | null>(null)
  const [torch, setTorch] = React.useState(false)

  const openCamera = async () => {
    if (Platform.OS === 'android') {
      const granted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA)
      if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
        Alert.alert('Permiso requerido', 'Se necesita permiso de cámara para continuar.')
        return
      }
    }
    setScreen('camera')
  }

  if (screen === 'home') {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 }}>
        <Pressable onPress={openCamera} style={{ padding: 14, backgroundColor: '#0af' }}>
          <Text style={{ color: '#fff' }}>Abrir cámara</Text>
        </Pressable>
      </View>
    )
  }

  if (screen === 'camera') {
    return (
      <View style={{ flex: 1 }}>
        <QRScannerView
          style={{ flex: 1 }}
          active={true}
          torchOn={torch}
          onCodeScanned={(value) => {
            setQr(value)
            setScreen('result')
          }}
        />
        <View style={{ position: 'absolute', bottom: 40, left: 0, right: 0, alignItems: 'center' }}>
          <Pressable onPress={() => setTorch((t) => !t)} style={{ padding: 10, backgroundColor: '#222' }}>
            <Text style={{ color: '#fff' }}>{torch ? 'Apagar' : 'Prender'} flash</Text>
          </Pressable>
          <Pressable onPress={() => setScreen('home')} style={{ padding: 10, backgroundColor: '#444', marginTop: 8 }}>
            <Text style={{ color: '#fff' }}>Cerrar</Text>
          </Pressable>
        </View>
      </View>
    )
  }

  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>QR leído:</Text>
      <Text style={{ fontWeight: 'bold', marginTop: 10 }}>{qr}</Text>
      <Pressable onPress={() => setScreen('home')} style={{ padding: 12, backgroundColor: '#0af', marginTop: 20 }}>
        <Text style={{ color: '#fff' }}>Volver</Text>
      </Pressable>
    </View>
  )
}
