# @noemec/react-native-qr-camera

Ultra-ligera cámara para abrir la cámara, leer un QR y emitir su valor. Sin estilos, con control de flash y manejo de sesión correcto.

Estado actual
- Android: Preview (Camera2) + flash + lectura de QR (ZXing vendoreado). Probado y funcionando.
- iOS: Implementado (AVFoundation) pero aún no se ha probado de extremo a extremo en dispositivos reales.

## Instalación

1) Instala el paquete en tu app:
```
npm i @noemec/react-native-qr-camera
```

2) iOS: instala pods en tu app:
```
cd ios && pod install && cd ..
```

3) Autolinking de RN hace el resto.

4) Android: ZXing core está vendoreado como JAR en `android/libs/core-3.5.3.jar` para decodificar QR sin dependencias externas.

Permisos
- iOS: añade `NSCameraUsageDescription` en tu Info.plist.
- Android: la app debe solicitar `android.permission.CAMERA` antes de montar el componente.

## API
```ts
import { QRScannerView } from '@noemec/react-native-qr-camera'

<QRScannerView
  style={{ flex: 1 }}
  active={true}
  torchOn={false}
  onCodeScanned={(value) => {
    // valor del QR
  }}
/>
```
Props
- active: boolean — inicia/detiene la sesión de cámara (por defecto true)
- torchOn: boolean — enciende/apaga el flash si disponible (por defecto false)
- onCodeScanned: (value: string) => void — se llama al detectar un QR

## Ejemplo de navegación (sin librerías externas)
Revisa `example/App.tsx` con 3 pantallas simples usando estado local.

## Notas
- Android utiliza ZXing core vendoreado (JAR local) para mantener el módulo ligero.
- iOS está implementado, pero aún no validado en producción. Por ahora, este paquete ha sido probado solo en Android.

## Licencia
MIT
