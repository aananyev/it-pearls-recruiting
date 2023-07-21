postgre_database=/usr/local/var/postgresql@11
old_archive=../it-pearls-basebackup-old.tgz
new_archive=../it-pearls-basebackup.tgz
postgre_database=/usr/local/var/postgresql@11
BACKUPBASELOG=backupbase.log
db_server=hr.it-pearls.ru

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
pg_ctl stop -D . >> $LOG 2>/dev/null
if [ $? -eq 0 ]; then 
	echo "\033[32mOK"
else
	echo "\033[31mБаза данных не запущена: $?"
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

echo "\033[37mУдаление старой базы ... \c"
rm -rf *
if [ $? -eq 0 ]; then
        echo "\033[32mOK"
else
        echo "\033[31mFailure, exit status: $?"
fi
echo "\033[37mЗагрузка базы с сервера ... "
pg_basebackup -P -h $db_server -D . -U replica  >>$LOG
if [ $? -eq 0 ]; then
	echo "\033[32mOK"
else
	echo "\033[31mОшибка загрузки базы: $? Проверьте интернет или настройки сервера баз данных $db_server"
	echo "\033[37mВосстановить базу из архива $new_archive ...\c"
	tar xvf $new_archive 1>/dev/null 2>$LOG

	if [ $? -eq 0 ]; then
		echo "\033[32mOK"

		echo "\033[37mЗапуск старой базы ...\c"
		pg_ctl start -D . >> $LOG

                if [ $? -eq 0 ]; then
                        echo "\033[32mOK"
                fi

		echo "\033[37mВосстановить исходные архивы ...\c"
		rm $new_archive
		mv $old_archive $new_archive
		if [ $? -eq 0 ]; then
			echo "\033[32mOK"
		fi
	fi

	cd $CWD
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
