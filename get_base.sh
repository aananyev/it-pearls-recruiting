postgre_database=/usr/local/var/postgresql@11
old_archive=../it-pearls-basebackup-old.tgz
new_archive=../it-pearls-basebackup.tgz
postgre_database=/usr/local/var/postgresql@11

echo "Переход в каталог базы $postgre_database" 
cd $postgre_database
if [ $? -eq 0 ]; then
    echo OK
else
    echo "Нет каталога базы PostgreSQL " $postgre_database
    echo FAIL
fi
echo "Остановка локальной базы ..."
pg_ctl stop -D .
echo "Архивация старой базы ..."
rm $old_archive
mv $new_archive $old_archive
tar zcvf $new_archive * > /dev/null
echo "Удаление старой базы ..."
rm -rf *
echo "Загрузка базы с сервера ..."
pg_basebackup -h hr.it-pearls.ru -D . -U replica
echo "Запуск базы ..."
pg_ctl start -D .
