import * as React from 'react'
import { requireNativeComponent, StyleProp, ViewStyle } from 'react-native'

export type QRScannerViewProps = {
  style?: StyleProp<ViewStyle>
  active?: boolean
  torchOn?: boolean
  onCodeScanned?: (value: string) => void
}

type NativeProps = {
  style?: StyleProp<ViewStyle>
  active?: boolean
  torchOn?: boolean
  onCodeScanned?: (event: { nativeEvent: { value: string } }) => void
}

const COMPONENT_NAME = 'NoemecQRScannerView'

const NativeQRScannerView = requireNativeComponent<NativeProps>(COMPONENT_NAME)

export const QRScannerView: React.FC<QRScannerViewProps> = ({
  style,
  active = true,
  torchOn = false,
  onCodeScanned,
}) => {
  const handleEvent = React.useCallback<NonNullable<NativeProps['onCodeScanned']>>(
    (e) => {
      onCodeScanned?.(e.nativeEvent.value)
    },
    [onCodeScanned]
  )

  return (
    <NativeQRScannerView
      style={style as any}
      active={active}
      torchOn={torchOn}
      onCodeScanned={handleEvent}
    />
  )
}

export default QRScannerView
