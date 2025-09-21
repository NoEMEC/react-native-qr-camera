require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "noemec-react-native-qr-camera"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = { :type => package['license'] }
  s.authors      = { "NoEMEC" => "" }
  s.homepage     = "https://github.com/NoEMEC/react-native-qr-camera"
  s.source       = { :git => "https://github.com/NoEMEC/react-native-qr-camera.git", :tag => s.version }

  s.platforms    = { :ios => "12.0" }
  s.source_files = "ios/**/*.{h,m,mm}"
  s.dependency 'React-Core'
end
