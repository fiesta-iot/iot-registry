# FIESTA-IoT IoT-Registry
FIESTA-IoT IoT-Registry is the component responsible for storing the semantic descriptions from the resources and observations the federated testbed export.

## Requirements
### Java
It is require to have Java 8 or later available.
```
$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java8-installer
```
As the internal entity identifiers generated are based on encription algorithms, it is also necessary to include JCE policy for specific security algorithms.
```
$ sudo apt-get install oracle-java8-unlimited-jce-policy
```

### WildFly
For installing WildFly 10, we have used the script provided by [Dmitriy Sukharev](https://gist.github.com/sukharevd/6087988). Feel free to modify the configuration to fit your own requimentes. At the end of the script we offer the option to restrict connections, etc.
```
$ cat > wildfly.sh << EOF
#!/bin/bash
#title           :wildfly-install.sh
#description     :The script to install Wildfly 10.x
#more            :http://sukharevd.net/wildfly-8-installation.html
#author          :Dmitriy Sukharev (modified by Jorge Lanza)
#date            :2016-06-18T02:45-0700
#usage           :/bin/bash wildfly-install.sh
#tested-version1 :10.0.0.CR3
#tested-distros1 :Ubuntu 15.10; Debian 7,8; CentOS 7; Fedora 22
#tested-version2 :10.0.0.Final
#tested-distros2 :Debian 8

WILDFLY_VERSION=10.0.0.Final
WILDFLY_FILENAME=wildfly-$WILDFLY_VERSION
WILDFLY_ARCHIVE_NAME=$WILDFLY_FILENAME.tar.gz
WILDFLY_DOWNLOAD_ADDRESS=http://download.jboss.org/wildfly/$WILDFLY_VERSION/$WILDFLY_ARCHIVE_NAME

INSTALL_DIR=/opt
WILDFLY_FULL_DIR=$INSTALL_DIR/$WILDFLY_FILENAME
WILDFLY_DIR=$INSTALL_DIR/wildfly

WILDFLY_USER="wildfly"
WILDFLY_SERVICE="wildfly"
WILDFLY_MODE="standalone"

WILDFLY_STARTUP_TIMEOUT=240
WILDFLY_SHUTDOWN_TIMEOUT=30

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root."
   exit 1
fi

echo "Downloading: $WILDFLY_DOWNLOAD_ADDRESS..."
[ -e "$WILDFLY_ARCHIVE_NAME" ] && echo 'Wildfly archive already exists.'
if [ ! -e "$WILDFLY_ARCHIVE_NAME" ]; then
  wget -q $WILDFLY_DOWNLOAD_ADDRESS
  if [ $? -ne 0 ]; then
    echo "Not possible to download Wildfly."
    exit 1
  fi
fi

echo "Cleaning up..."
rm -f "$WILDFLY_DIR"
rm -rf "$WILDFLY_FULL_DIR"
rm -rf "/var/run/$WILDFLY_SERVICE/"
rm -f "/etc/init.d/$WILDFLY_SERVICE"

echo "Installation..."
mkdir $WILDFLY_FULL_DIR
tar -xzf $WILDFLY_ARCHIVE_NAME -C $INSTALL_DIR
ln -s $WILDFLY_FULL_DIR/ $WILDFLY_DIR
useradd -s /sbin/nologin $WILDFLY_USER
chown -R $WILDFLY_USER:$WILDFLY_USER $WILDFLY_DIR
chown -R $WILDFLY_USER:$WILDFLY_USER $WILDFLY_DIR/

#mkdir -p /var/log/$WILDFLY_SERVICE

echo "Registrating Wildfly as service..."
# if should use systemd
if [ -x /bin/systemctl ]; then
    # Script from $WILDFLY_DIR/docs/contrib/scripts/systemd/launch.sh didn't work for me
    cat > $WILDFLY_DIR/bin/launch.sh << "EOF"
#!/bin/sh

if [ "x$WILDFLY_HOME" = "x" ]; then
    WILDFLY_HOME="/opt/wildfly"
fi

if [ "x$1" = "xdomain" ]; then
    echo 'Starting Wildfly in domain mode.'
    $WILDFLY_HOME/bin/domain.sh -c $2 -b $3
    #>> /var/log/$WILDFLY_SERVICE/server-`date +%Y-%m-%d`.log
else
    echo 'Starting Wildfly in standalone mode.'
    $WILDFLY_HOME/bin/standalone.sh -c $2 -b $3
    #>> /var/log/$WILDFLY_SERVICE/server-`date +%Y-%m-%d`.log
fi
EOF
    # $WILDFLY_HOME is not visible here
    sed -i -e 's,WILDFLY_HOME=.*,WILDFLY_HOME='$WILDFLY_DIR',g' $WILDFLY_DIR/bin/launch.sh
    #sed -i -e 's,$WILDFLY_SERVICE,'$WILDFLY_SERVICE',g' $WILDFLY_DIR/bin/launch.sh
    chmod +x $WILDFLY_DIR/bin/launch.sh

    cp $WILDFLY_DIR/docs/contrib/scripts/systemd/wildfly.service /etc/systemd/system/$WILDFLY_SERVICE.service
    WILDFLY_SERVICE_CONF=/etc/default/$WILDFLY_SERVICE
    # To install multiple instances of Wildfly replace all hardcoding in systemd file
    sed -i -e 's,EnvironmentFile=.*,EnvironmentFile='$WILDFLY_SERVICE_CONF',g' /etc/systemd/system/$WILDFLY_SERVICE.service
    sed -i -e 's,User=.*,User='$WILDFLY_USER',g' /etc/systemd/system/$WILDFLY_SERVICE.service
    sed -i -e 's,PIDFile=.*,PIDFile=/var/run/wildfly/'$WILDFLY_SERVICE'.pid,g' /etc/systemd/system/$WILDFLY_SERVICE.service
    sed -i -e 's,ExecStart=.*,ExecStart='$WILDFLY_DIR'/bin/launch.sh $WILDFLY_MODE $WILDFLY_CONFIG $WILDFLY_BIND,g' /etc/systemd/system/$WILDFLY_SERVICE.service
    systemctl daemon-reload
    #systemctl enable $WILDFLY_SERVICE.service
fi

# if non-systemd Debian-like distribution
if [ ! -x /bin/systemctl -a -r /lib/lsb/init-functions ]; then
    cp $WILDFLY_DIR/docs/contrib/scripts/init.d/wildfly-init-debian.sh /etc/init.d/$WILDFLY_SERVICE
    sed -i -e 's,NAME=wildfly,NAME='$WILDFLY_SERVICE',g' /etc/init.d/$WILDFLY_SERVICE
    WILDFLY_SERVICE_CONF=/etc/default/$WILDFLY_SERVICE
fi

# if non-systemd RHEL-like distribution
if [ ! -x /bin/systemctl -a -r /etc/init.d/functions ]; then
    cp $WILDFLY_DIR/docs/contrib/scripts/init.d/wildfly-init-redhat.sh /etc/init.d/$WILDFLY_SERVICE
    WILDFLY_SERVICE_CONF=/etc/default/wildfly.conf
    chmod 755 /etc/init.d/$WILDFLY_SERVICE
fi

# if neither Debian nor RHEL like distribution
if [ ! -x /bin/systemctl -a ! -r /lib/lsb/init-functions -a ! -r /etc/init.d/functions ]; then
cat > /etc/init.d/$WILDFLY_SERVICE << "EOF"
#!/bin/sh
### BEGIN INIT INFO
# Provides:          ${WILDFLY_SERVICE}
# Required-Start:    $local_fs $remote_fs $network $syslog
# Required-Stop:     $local_fs $remote_fs $network $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start/Stop ${WILDFLY_FILENAME}
### END INIT INFO

WILDFLY_USER=${WILDFLY_USER}
WILDFLY_DIR=${WILDFLY_DIR}

case "$1" in
start)
echo "Starting ${WILDFLY_FILENAME}..."
start-stop-daemon --start --background --chuid $WILDFLY_USER --exec $WILDFLY_DIR/bin/standalone.sh
exit $?
;;
stop)
echo "Stopping ${WILDFLY_FILENAME}..."

start-stop-daemon --start --quiet --background --chuid $WILDFLY_USER --exec $WILDFLY_DIR/bin/jboss-cli.sh -- --connect command=:shutdown
exit $?
;;
log)
echo "Showing server.log..."
tail -500f $WILDFLY_DIR/standalone/log/server.log
;;
*)
echo "Usage: /etc/init.d/wildfly {start|stop}"
exit 1
;;
esac
exit 0
EOF
sed -i -e 's,${WILDFLY_USER},'$WILDFLY_USER',g; s,${WILDFLY_FILENAME},'$WILDFLY_FILENAME',g; s,${WILDFLY_SERVICE},'$WILDFLY_SERVICE',g; s,${WILDFLY_DIR},'$WILDFLY_DIR',g' /etc/init.d/$WILDFLY_SERVICE
chmod 755 /etc/init.d/$WILDFLY_SERVICE
fi

if [ ! -z "$WILDFLY_SERVICE_CONF" ]; then
    echo "Configuring service..."
    echo JBOSS_HOME=\"$WILDFLY_DIR\" > $WILDFLY_SERVICE_CONF
    echo JBOSS_USER=$WILDFLY_USER >> $WILDFLY_SERVICE_CONF
    echo WILDFLY_HOME=\"$WILDFLY_DIR\" > $WILDFLY_SERVICE_CONF
    echo WILDFLY_USER=\"$WILDFLY_USER\" > $WILDFLY_SERVICE_CONF
    echo STARTUP_WAIT=$WILDFLY_STARTUP_TIMEOUT >> $WILDFLY_SERVICE_CONF
    echo SHUTDOWN_WAIT=$WILDFLY_SHUTDOWN_TIMEOUT >> $WILDFLY_SERVICE_CONF
    echo WILDFLY_CONFIG=$WILDFLY_MODE.xml >> $WILDFLY_SERVICE_CONF
    echo WILDFLY_MODE=$WILDFLY_MODE >> $WILDFLY_SERVICE_CONF
    echo WILDFLY_BIND=0.0.0.0 >> $WILDFLY_SERVICE_CONF
fi

echo "Configuring application server..."
cp $WILDFLY_DIR/$WILDFLY_MODE/configuration/$WILDFLY_MODE.xml $WILDFLY_DIR/$WILDFLY_MODE/configuration/$WILDFLY_MODE_orig.xml
sed -i -e 's,<deployment-scanner path="deployments" relative-to="jboss.server.base.dir" scan-interval="5000",<deployment-scanner path="deployments" relative-to="jboss.server.base.dir" scan-interval="5000" deployment-timeout="'$WILDFLY_STARTUP_TIMEOUT'",g' $WILDFLY_DIR/$WILDFLY_MODE/configuration/$WILDFLY_MODE.xml

# Wildfly will only allow connections through local host. Otherwise remove the lines below as desired for administration and use.
#sed -i -e 's,<inet-address value="${jboss.bind.address.management:127.0.0.1}"/>,<any-address/>,g' $WILDFLY_DIR/$WILDFLY_MODE/configuration/$WILDFLY_MODE.xml
#sed -i -e 's,<inet-address value="${jboss.bind.address:127.0.0.1}"/>,<any-address/>,g' $WILDFLY_DIR/$WILDFLY_MODE/configuration/$WILDFLY_MODE.xml

[ -x /bin/systemctl ] && systemctl start $WILDFLY_SERVICE || service $WILDFLY_SERVICE start

echo "Done."
EOF
```
Now, run the script as `root`.
```
$ sudo su -c './wildfly.sh'
```

Next we need to create the administrator user.
```
$ cd /opt/wildfly/bin/
$ sudo -u wildfly ./add-user.sh
```

### MySQL 5.7 or higher
We are asumming the use of Ubuntu 14.04, which provides MySQL 5.5. You can also modify the initialization script, but it is suggested to use the latest versions of MySQL.

In case you have a previous MySQL installation, we highly recommend to make a backup of the databases.
```
$ mysqldump --lock-all-tables -u root -p --all-databases > dump.sql
```
Then, delete the old MySQL version and proceed with the installation of the new one.
```
$ sudo apt-get purge mysql-server-5.5 mysql-client-5.5
$ sudo apt-get autoremove
$ wget http://dev.mysql.com/get/mysql-apt-config_0.8.0-1_all.deb
$ sudo dpkg -i mysql-apt-config_0.8.0-1_all.deb
$ sudo apt-get update
$ sudo apt-get install mysql-server
$ sudo mysql_secure_installation
```

By default former databases are migrated, but just in case something went wrong you always have your backup.
```
$ mysql -u root -p < dump.sql
```

## Installation and configuration
Clone this repository.


### Create FIESTA-IoT IoT-Registry SQL database

 Before you run the SQL script, modify username, password and database name in case you'd like to. Keep in mind that afterwards you should do the same modifications before deploying the IoT-Registry.
```
$ mysql -u root -p < database_structure.sql
```

### Compile and deploy

Configure the IoT-Registry by modifying the properties files under `conf`.

- `wildfly.properties`: WildFly management console parameters
- `persistence.properties`: database credentials and location
- `fiesta-iot.properties`: specific IoT-Registry parameters

Create the folder where the triple store is to be located. It has to be the same location as the one included in `fiesta-iot.properties`.
```
$ sudo mkdir -p /var/opt/wildfly/iot-registry/triple-store
$ sudo chown -R wildfly:wildfly /var/opt/wildfly/
```

#### Non SSL WildFly management console

In `wilfly.propeties` include the `http-remoting` as `wildfly.protocol` property.
```
wildfly.protocol=http-remoting
```
Do not forget to include hostname and port, and username and password.

Run maven to compile and deploy.
```
$ mvn initialize wildfly:deploy
```

#### SSL WildFly management console
In case the WildFly server is configure to support SSL connections only, then you have to configure Maven to use the required properties and paramter including the server certificate, etc.

In `wilfly.propeties` include the `https-remoting` as `wildfly.protocol` property.
```
wildfly.protocol = https-remoting
```

Do not forget to include hostname and port, and username and password.

Assuming you have OpenSSL installed, in order to retrive the full server certificate chain run:
```
$ openssl s_client -showcerts -connect platform.fiesta-iot.eu:443 < /dev/null | \
awk -v c=-1 ’/-----BEGIN CERTIFICATE-----/{inc=1;c++}
             inc {print > ("level" c ".crt")}
             /---END CERTIFICATE-----/{inc=0}’
$ ls level*.crt
```
Note that the server address has to be set to your specific one.

Each of the `level*.crt` files contains the certificates from the server's certificate chain. You can merge all in just one file.
```
$ cat level*.crt > server_cert_chain.crt
```

Now it's time to create the trust keystore for Maven. You can also import each certificate individually.
```
$ keytool -v -alias server_wildfly -import -file server_cert_chain.crt -keystore fiesta-iot_trust.jks
```

In order to easy the execution of the maven command and include the new keystore, we need to define `MAVEN_OPTS` environment variable. You can also use `.mavenrc`.
```
$ export MAVEN_OPTS="-Xmx512m -Djavax.net.ssl.trustStore=fiesta-iot_trust.jks \
                              -Djavax.net.ssl.trustStorePassword=YOUR_KEYSTORE_PASSWORD"
```

Then in the same shell (unless you include the export otherwhere else), run maven to compile and deploy.
```
$ mvn initialize wildfly:deploy
```


### Configuration after deployment

In case you want to modify the IoT-Registry parameters without recompiling and redeploying you can copy `fiesta-iot.properties` to WildFly configuration directory (usually `$WILDFLY_HOME/standalone/configuration`)
```
$ sudo -u wildfly cp fiesta-iot.properties /opt/wildfly/standalone/configuration/
$ sudo -u wildfly chown wildfly:wildfly fiesta-iot.properties

```

You are done.

## Authors
The FIESTA-IoT IoT-Registry component has been written by:

- [Jorge Lanza](https://github.com/jlanza)
- David Gomez
- Luis Sanchez
- [Juan Ramon Santana](https://github.com/juanrasantana)

## Acknowledgements
This work is partially funded by the European project Federated Interoperable Semantic IoT cloud Testbeds and Applications (FIESTA-IoT) from the European Union’s Horizon 2020 Programme with the Grant Agreement No. CNECT-ICT-643943. The authors would also like to thank the FIESTA-IoT consortium for the fruitful discussions.

