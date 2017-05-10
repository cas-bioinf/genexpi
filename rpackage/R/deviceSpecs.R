getDeviceSpecs <- function(device = NULL, deviceType = NULL) {
  if(is.null(device)) {
    if (is.null(deviceType)) {
      return (.jnew(rinterfaceJavaType("DeviceSpecs")));

    }
    else {
      if (deviceType == "gpu") {
        return (.jcall(rinterfaceJavaType("DeviceSpecs"), rinterfaceJavaReturnType("DeviceSpecs"), "gpuSpecs"))
      }
      else if (deviceType == "processor") {
        return (.jcall(rinterfaceJavaType("DeviceSpecs"), rinterfaceJavaReturnType("DeviceSpecs"), "processorSpecs"))
      }
      else {
        stop("deviceType has to be either 'gpu' or 'processor'");
      }

    }
  }
  else {
    if(!is.null(deviceType)) {
      stop("You cannot specify both device and deviceType");
    }

    deviceList = J(rinterfaceJavaType("RInterface"))$getAllDevices();
    numDevices = deviceList$size();
    if(is.integer(device)) {
      if(device < 0 || device >= numDevices) {
        stop("Invalid device ID. Call XXX to get the list of possible devices"); #TODO method name
      }
      return (new(J(rinterfaceJavaType("DeviceSpecs"), deviceList$get(device))));
    }
  }

}
