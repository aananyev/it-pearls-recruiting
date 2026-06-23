# CUBA 7.3 requires Java 8 or 11 for Tomcat (Java 17+ breaks UI route scanning).
# Always override JAVA_HOME — a shell/IDE may export Java 17/22 and break UiControllerResourceMeta.
if [ -d "/Users/alekseyananyev/Library/Java/JavaVirtualMachines/corretto-11.0.17/Contents/Home" ]; then
  export JAVA_HOME="/Users/alekseyananyev/Library/Java/JavaVirtualMachines/corretto-11.0.17/Contents/Home"
else
  export JAVA_HOME="$(/usr/libexec/java_home -v 11 2>/dev/null || /usr/libexec/java_home -v 1.8 2>/dev/null)"
fi
export JRE_HOME="$JAVA_HOME"

CATALINA_OPTS="-Xmx1024m -Dfile.encoding=UTF-8 -Dapp.home=\"$CATALINA_BASE/../app_home\""

CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote"

JPDA_OPTS="-Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"
