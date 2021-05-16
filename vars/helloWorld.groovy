def call(Map config= [:])
{ 
sh "echo Hello World ${config.name} and Today is ${config.dayOfWeek}"
}
