getDeviceSpecs <- function(device = NULL, deviceType = NULL) {
  deviceSpecs = list(device = device, deviceType = deviceType);
  class(deviceSpecs) <- "genexpiDeviceSpecs";
  #Will stop if the input is invalid
  getJavaDeviceSpecs(deviceSpecs);
  return(deviceSpecs);
}

getJavaDeviceSpecs <- function(deviceSpecs) {
  if(class(deviceSpecs) ==  "jobjRef") {
    return(deviceSpecs)
  }
  else if(class(deviceSpecs) ==  "genexpiDeviceSpecs") {
    device = deviceSpecs$device;
    deviceType = deviceSpecs$deviceType;
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
      device = as.integer(device);
      if(length(device) > 1) {
        stop("device has to be coercible to a single integer");
      }
      if(device < 1 || device > numDevices) {
        stop("Invalid device ID. Call listOpenCLDevices() to get the list of possible devices");
      }
      return (new(J(rinterfaceJavaType("DeviceSpecs")), deviceList$get(as.integer(device - 1))));
    }

  }
  else {
    stop("Invalid input must be Java object or a an instance of the genexpiDeviceSpecs class");
  }
}


listOpenCLDevices <- function() {
  deviceList = J(rinterfaceJavaType("RInterface"))$getAllDevices();
  numDevices = deviceList$size();
  result = character(numDevices);
  for(i in 1:numDevices) {
    result[i] = deviceList$get(as.integer(i - 1))$toString();
  }
  return(result);
}
