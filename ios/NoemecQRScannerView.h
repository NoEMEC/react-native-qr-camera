#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <React/RCTComponent.h>

@interface NoemecQRScannerView : UIView <AVCaptureMetadataOutputObjectsDelegate>

@property (nonatomic, assign) BOOL active;
@property (nonatomic, assign) BOOL torchOn;
@property (nonatomic, copy) RCTBubblingEventBlock onCodeScanned;

@end
