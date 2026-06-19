# CUBA 7.3 requires Java 8 or 11 for Tomcat (Java 17+ breaks UI route scanning).
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 11 2>/dev/null)}"
export JRE_HOME="$JAVA_HOME"

CATALINA_OPTS="-Xmx512m -Dfile.encoding=UTF-8 -Dapp.home=\"$CATALINA_BASE/../app_home\""

CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote"

JPDA_OPTS="-Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"
