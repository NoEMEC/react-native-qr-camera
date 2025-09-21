#import <React/RCTViewManager.h>
#import "NoemecQRScannerView.h"

@interface NoemecQRScannerViewManager : RCTViewManager
@end

@implementation NoemecQRScannerViewManager

RCT_EXPORT_MODULE(NoemecQRScannerView)

- (UIView *)view
{
  return [NoemecQRScannerView new];
}

RCT_EXPORT_VIEW_PROPERTY(active, BOOL)
RCT_EXPORT_VIEW_PROPERTY(torchOn, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onCodeScanned, RCTBubblingEventBlock)

@end
