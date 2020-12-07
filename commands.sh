xterm -e "./barrier.py --host 127.0.0.1 --port 11100 --processes 4" &
xterm -e "./finishedSignal.py --host 127.0.0.1 --port 11200 --processes 4" &
xterm -e "./run.sh --id 1 --hosts hosts_config_4 --barrier 127.0.0.1:11100 --signal 127.0.0.1:11200 --output output_1 config_lcb" &
xterm -e "./run.sh --id 2 --hosts hosts_config_4 --barrier 127.0.0.1:11100 --signal 127.0.0.1:11200 --output output_2 config_lcb" &
xterm -e "./run.sh --id 3 --hosts hosts_config_4 --barrier 127.0.0.1:11100 --signal 127.0.0.1:11200 --output output_3 config_lcb" &
xterm -e "./run.sh --id 4 --hosts hosts_config_4 --barrier 127.0.0.1:11100 --signal 127.0.0.1:11200 --output output_4 config_lcb" &
