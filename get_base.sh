postgre_database=/usr/local/var/postgresql@11
old_archive=../it-pearls-basebackup-old.tgz
new_archive=../it-pearls-basebackup.tgz
postgre_database=/usr/local/var/postgresql@11
BACKUPBASELOG=backupbase.log

CWD=$(pwd)

LOG=$CWD/$BACKUPBASELOG

echo "\033[37mПереход в каталог базы $postgre_database ... \c" 
cd $postgre_database
if [ $? -eq 0 ]; then
    	echo "\033[32mOK"
else
    	echo "\033[31mНет каталога базы PostgreSQL " $postgre_database
    	echo FAIL
	exit 1
fi

echo "\033[37mОстановка локальной базы ... \c"
pg_ctl stop -D . >> $LOG
if [ $? -eq 0 ]; then 
	echo "\033[32mOK"
else
	echo "\033[31mFailure, exit status: $?"
fi

echo "\033[37mАрхивация старой базы ... \c"
rm $old_archive
mv $new_archive $old_archive
tar zcvf $new_archive * 1>>$LOG 2>> $LOG 
if [ $? -eq 0 ]; then
	echo "\033[32mOK"
else
	echo "\033[31mFailure, exit status: $?"
	exit 1
fi

echo "\033[37mУдаление старой базы ... \c"
rm -rf *
if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mFailure, exit status: $?"
fi
echo "\033[37mЗагрузка базы с сервера ... \c"
pg_basebackup -P -h hr.it-pearls.ru -D . -U replica 
if [ $? -eq 0 ]; then
	echo "\033[32mOK"
else
	echo "\033[31mFailure, exit status: $?"
	exit 1
fi

echo "\033[37mЗапуск базы ... \c"
pg_ctl start -D . >> $LOG 
if [ $? -eq 0 ]; then
	echo "\033[32mOK"
else
	echo "\033[31mFailure, exit status: $?"
	exit 1
fi

cd $CWD
