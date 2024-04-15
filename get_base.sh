postgre_version=11
postgre_subversion=22_1
postgre_database=/usr/local/var/postgresql@i$postgre_version
current_catalog=`pwd`
old_archive=../it-pearls-basebackup-old.$postgre_version.$postgre_subversion.tgz
new_archive=../it-pearls-basebackup.$postgre_version.$postgre_subversion.tgz
postgre_database=/usr/local/var/postgresql@$postgre_version
postgre_temp_database=`echo $current_catalog"/postgre_tmp_database"`
BACKUPBASELOG=backupbase.log
db_server=hr.it-pearls.ru
PG_CTL=/usr/local/Cellar/postgresql@$postgre_version/$postgre_version.$postgre_subversion/bin/pg_ctl
PG_BASEBACKUP=/usr/local/Cellar/postgresql@$postgre_version/$postgre_version.$postgre_subversion/bin/pg_basebackup

CWD=$(pwd)

LOG=$CWD/$BACKUPBASELOG

echo "*******************************************************"
echo "*******************************************************"
echo "**                                                   **"
echo "**                   HuntTech                        **"
echo "**       Загрузка базы из основной площадки          **"
echo "**                                                   **"
echo "*******************************************************"
echo "*******************************************************"
echo "\033[37mОсновная площадка: \033[32m$db_server"
echo "\033[37mВерсия postgresql: \033[32m$postgre_version.$postgre_subversion"
echo "\033[37mСоздание временного каталога базы Postgre $postgre_temp_database ...\c"
mkdir $postgre_temp_database
if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
	echo "\033[31mНе удалось создать временный каталог базы $postgre_temp_database ..."
fi

echo "\033[37mПереход во временный каталог базы $postgre_temp_database ...\c" 
cd $postgre_temp_database
if [ $? -eq 0 ]; then
    	echo "\033[32mOK"
else
    	echo "\033[31mНе могу зайти во временный каталог базы PostgreSQL $postgre_temp_database ..."
    	echo FAIL
	exit 1
fi

echo "\033[37mЗагрузка базы с сервера ..."
# $postgre_pg_basebackup -P -h $db_server -D . -U replica  >>$LOG
$PG_BASEBACKUP -P -h $db_server -D . -U replica  >>$LOG
if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[37mНе удалось загрузить базу с сервера во временный каталог ..."
        echo FAIL
        exit 1
fi

echo "\033[37mПереход в каталог базы $postgre_database ...\c"
cd $postgre_database
if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mНе могу зайти в каталог базы PostgreSQL $postgre_database"
        echo FAIL
        exit 1
fi

echo "\033[37mОстановка локальной базы ... \c"
$PG_CTL stop -D . >> $LOG 2>/dev/null

if [ $? -eq 0 ]; then 
	echo "\033[32mOK"
else
	echo "\033[31mБаза данных не запущена: $?"
fi

echo "\033[37mУдаление старой базы ... \c"
rm -rf *
if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mFailure, exit status: $?"
fi

echo "\033[37mКопирование базы из временного каталога$postgre_temp_database в каталог $postgre_database ..."
cp -av $postgre_temp_database/* . | pv -l -s `ls -laR $postgre_temp_database | grep "^-" | wc -l` > logfile

if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mНе удалось скопировать базу из временного каталога $postgre_temp_database в постоянный каталог базы $postgre_database ..."
	echo FAIL
	exit 1
fi

echo "\033[37mУдаление базы из временного каталога $postgre_temp_database ... \c"
rm -rf $postgre_temp_database

if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mНе удалось удалить базу из временного каталога $postgre_temp_database"
        echo FAIL
        exit 1
fi

echo "\033[37mАрхивация старой базы ... "
rm $old_archive 2>>$LOG 1>/dev/null
mv $new_archive $old_archive 2>>$LOG 1>/dev/null
# tar zcvf $new_archive * 1>>$LOG 2>> $LOG 
tar czf - * | (pv -p --timer --rate --bytes > $new_archive) 
if [ $? -eq 0 ]; then
	echo "\033[32mOK"
else
	echo "\033[31mFailure, exit status: $?"
	exit 1
fi

echo "\033[37mЗапуск базы ... \c"
$PG_CTL start -D . >> $LOG 
if [ $? -eq 0 ]; then
	echo "\033[32mOK"
else
	echo "\033[31mFailure, exit status: $?"
	exit 1
fi

echo "\033[37mКопирование файлов из хранилища в локальное ... "
# scp -c -v root@hr.it-pearls.ru:/opt/app_home/fileStorage/* /opt/app_home/
rsync -avrltD --stats --ignore-existing root@hr.it-pearls.ru:/opt/app_home/fileStorage /opt/app_home/ | pv --timer -lep -s 5 > /dev/null

if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mFailure, exit status: $?"
        exit 1
fi

cd $CWD
