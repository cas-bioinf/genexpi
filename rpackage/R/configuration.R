genexpiOptions <- function(verbose = NULL) {
  if(!is.null(verbose)) {
    J(rinterfaceJavaType("RInterface"))$setVerbose(as.logical(verbose)[1]);
  }
}
