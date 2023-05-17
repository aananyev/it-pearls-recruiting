postgre_database=/usr/local/var/postgresql@11
old_archive=../it-pearls-basebackup-old.tgz
new_archive=../it-pearls-basebackup.tgz
postgre_database=/usr/local/var/postgresql@11
BACKUPBASELOG=backupbase.log

CWD=$(pwd)

LOG=$CWD/$BACKUPBASELOG

echo "Переход в каталог базы $postgre_database" 
cd $postgre_database
if [ $? -eq 0 ]; then
    	echo OK
else
    	echo "Нет каталога базы PostgreSQL " $postgre_database
    	echo FAIL
	exit 1
fi

echo "Остановка локальной базы ..."
pg_ctl stop -D . >> $LOG
if [ $? -eq 0 ]; then 
	echo "OK"
else
	echo Failure, exit status: $?
fi

echo "Архивация старой базы ..."
rm $old_archive
mv $new_archive $old_archive
tar zcvf $new_archive * 1 2 >> $LOG 
if [ $? -eq 0 ]; then
	echo "OK"
else
	echo Failure, exit status: $?
	exit 1
fi

echo "Удаление старой базы ..."
rm -rf *
echo "Загрузка базы с сервера ..."
pg_basebackup -P -h hr.it-pearls.ru -D . -U replica 
if [ $? -eq 0 ]; then
	echo "OK"
else
	echo Failure, exit status: $?
	exit 1
fi

echo "Запуск базы ..."
pg_ctl start -D . >> $LOG 
if [ $? -eq 0 ]; then
	echo "OK"
else
	echo Failure, exit status: $?
	exit 1
fi

cd $CWD
