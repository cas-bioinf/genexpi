rinterfaceType <- function (typeName) {
  return (paste0("cz/cas/mbu/genexpi/rinterface/", (gsub("\\.","/", typeName))));
}

computeType <- function (typeName) {
  return (paste0("cz/cas/mbu/genexpi/compute/", (gsub("\\.","/", typeName))));
}

rinterfaceReturnType <- function(typeName) {
  return (paste0("L", rinterfaceType(typeName), ";"));
}
