#import <Foundation/Foundation.h>
#import <React/RCTView.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTComponent.h>
#import <AVFoundation/AVFoundation.h>
#import "NoemecQRScannerView.h"

@interface NoemecQRScannerView()
@property (nonatomic, strong) AVCaptureSession *session;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *previewLayer;
@property (nonatomic, strong) AVCaptureDeviceInput *videoInput;
@property (nonatomic, strong) AVCaptureMetadataOutput *metadataOutput;
@property (nonatomic, assign) BOOL hasEmitted;
@end

@implementation NoemecQRScannerView

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    _active = YES;
    _torchOn = NO;
    _hasEmitted = NO;
    [self setupSession];
  }
  return self;
}

- (void)dealloc
{
  [self teardownSession];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  self.previewLayer.frame = self.bounds;
}

- (void)setupSession
{
  AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
  if (status == AVAuthorizationStatusNotDetermined) {
    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {}];
  }

  self.session = [[AVCaptureSession alloc] init];
  self.session.sessionPreset = AVCaptureSessionPresetHigh;

  NSError *error = nil;
  AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
  if (!device) { return; }

  self.videoInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
  if (error) { return; }
  if ([self.session canAddInput:self.videoInput]) {
    [self.session addInput:self.videoInput];
  }

  self.metadataOutput = [[AVCaptureMetadataOutput alloc] init];
  if ([self.session canAddOutput:self.metadataOutput]) {
    [self.session addOutput:self.metadataOutput];
    dispatch_queue_t queue = dispatch_queue_create("noemec.qr.metadata", DISPATCH_QUEUE_SERIAL);
    [self.metadataOutput setMetadataObjectsDelegate:self queue:queue];
    if ([self.metadataOutput.availableMetadataObjectTypes containsObject:AVMetadataObjectTypeQRCode]) {
      self.metadataOutput.metadataObjectTypes = @[AVMetadataObjectTypeQRCode];
    } else {
      self.metadataOutput.metadataObjectTypes = self.metadataOutput.availableMetadataObjectTypes;
    }
  }

  self.previewLayer = [AVCaptureVideoPreviewLayer layerWithSession:self.session];
  self.previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
  [self.layer addSublayer:self.previewLayer];

  if (self.active) {
    [self.session startRunning];
  }
}

- (void)teardownSession
{
  if (self.session) {
    if (self.session.isRunning) {
      [self.session stopRunning];
    }
  }
  self.previewLayer = nil;
  self.metadataOutput = nil;
  self.videoInput = nil;
  self.session = nil;
}

#pragma mark - Props

- (void)setActive:(BOOL)active
{
  if (_active == active) return;
  _active = active;
  if (active) {
    if (!self.session) {
      [self setupSession];
    }
    if (!self.session.isRunning) {
      [self.session startRunning];
    }
  } else {
    if (self.session.isRunning) {
      [self.session stopRunning];
    }
  }
}

- (void)setTorchOn:(BOOL)torchOn
{
  _torchOn = torchOn;
  AVCaptureDevice *device = self.videoInput.device;
  if ([device hasTorch] && [device isTorchModeSupported:AVCaptureTorchModeOn]) {
    NSError *error = nil;
    if ([device lockForConfiguration:&error]) {
      device.torchMode = torchOn ? AVCaptureTorchModeOn : AVCaptureTorchModeOff;
      [device unlockForConfiguration];
    }
  }
}

#pragma mark - Metadata Delegate

- (void)captureOutput:(AVCaptureOutput *)output didOutputMetadataObjects:(NSArray<__kindof AVMetadataObject *> *)metadataObjects fromConnection:(AVCaptureConnection *)connection
{
  if (self.onCodeScanned == nil) return;
  for (AVMetadataObject *metadata in metadataObjects) {
    if ([metadata isKindOfClass:[AVMetadataMachineReadableCodeObject class]]) {
      AVMetadataMachineReadableCodeObject *obj = (AVMetadataMachineReadableCodeObject *)metadata;
      if ([obj.type isEqualToString:AVMetadataObjectTypeQRCode]) {
        NSString *value = obj.stringValue ?: @"";
        if (value.length > 0) {
          dispatch_async(dispatch_get_main_queue(), ^{
            if (self.onCodeScanned) {
              self.onCodeScanned(@{ @"value": value });
            }
          });
        }
      }
    }
  }
}

@end
