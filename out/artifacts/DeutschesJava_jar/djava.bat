@echo off
for %%f in (DeutschesJava*.jar) do (
 set djar=%%f
 goto launch
)
:launch
java -jar %djar% %*