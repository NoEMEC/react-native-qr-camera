# @noemec/react-native-qr-camera

Ultra-ligera cámara para abrir la cámara, leer un QR y emitir su valor. Sin estilos, con control de flash y manejo de sesión correcto.

Estado actual
- Android: Preview (Camera2) + flash + lectura de QR (ZXing vendoreado). Probado y funcionando.
- iOS: Preview + lectura de QR (AVFoundation) + flash + teardown correcto. Probado en iPhone 13.

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

5) iOS: añade la clave de privacidad de la cámara (NSCameraUsageDescription) en tu app.

Con Xcode:

- Abre `ios/<TuProyecto>.xcworkspace`.
- Selecciona tu target de la app → pestaña Info.
- En “Custom iOS Target Properties”, pulsa “+” y agrega:
  - Key: NSCameraUsageDescription
  - Type: String
  - Value: La app necesita acceso a la cámara para escanear códigos QR.

O editando el Info.plist directamente:

```xml
<key>NSCameraUsageDescription</key>
<string>La app necesita acceso a la cámara para escanear códigos QR.</string>
```

Permisos
- iOS: añade `NSCameraUsageDescription` en Info.plist (ver pasos arriba). No se requiere micrófono.
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
- iOS requiere iOS 12+ (según el Podspec) y ha sido probado en dispositivos reales.

## Troubleshooting

- Crash en iOS al abrir la cámara con “Thread 11: SIGABRT” y mensaje sobre permisos: falta la clave `NSCameraUsageDescription` en el Info.plist. Añádela siguiendo los pasos de arriba.
- En simulador iOS la cámara puede estar limitada; prueba en dispositivo real.

## Licencia
MIT
