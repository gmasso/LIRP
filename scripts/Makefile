all: solve

solve: solve_lirp.sh
	./solve_lirp.sh &

kill: 
	kill -9 `ps -aux | grep 'bash' | awk '{print $2}'`
	kill -9 `ps -aux | grep 'java' | awk '{print $2}'`
