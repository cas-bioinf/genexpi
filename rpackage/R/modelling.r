
createModelFunc <- function(profiles, regulators)
{
  time = 1:(dim(profiles)[2]);
  numRegulators = length(regulators);
  profileApproximations = NULL;
  for (i in 1:numRegulators)
  {
    profileApproximations = c(profileApproximations, approxfun(time, profiles[regulators[i],]));
  }

  return (
    function(t, state, parameters)
    {
        if(numRegulators == 1)
        {
          regulatorValue = parameters["w"] * profileApproximations[[1]](t);
        }
        else
        {
          regulatorValue = 0;
          for (i in 1:numRegulators)
          {
            weight = parameters[paste0("w",i)];
            regulatorValue = regulatorValue + weight * profileApproximations[[i]](t);
          }
        }
        dY =  (parameters["k1"] / (1 + exp(-regulatorValue -parameters["b"]))) - parameters["k2"] * state["y"];
        return(list(dY));
    }
  );
}

predictProfile <- function(parameters, profiles, regulators, initialValue, time)
{
  state = c("y" = initialValue);
  modelFunc = createModelFunc(profiles, regulators)
  predicted = ode(state, time, modelFunc, parameters, method = "ode45")
  return (predicted[,2]);
}
