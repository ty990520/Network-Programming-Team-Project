#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#define BUF_SIZE 1024
void error_handling(char* message);

int main(int argc, char* argv[]) {
	int sock;
	char message[BUF_SIZE];
	int str_len;
	struct sockaddr_in serv_adr;

	if (argc != 3) {
		printf("Usage : %s [IP address] [Port]\n", argv[0]);
		exit(1);
	}

	sock = socket(PF_INET, SOCK_STREAM, 0);
	if (sock == -1) {
		error_handling("sock() error");
	}
	memset(&serv_adr, 0, sizeof(serv_adr));
	serv_adr.sin_family = AF_INET;
	serv_adr.sin_addr.s_addr = inet_addr(argv[1]);
	serv_adr.sin_port = htons(atoi(argv[2]));

	if (connect(sock, (struct sockaddr*)&serv_adr, sizeof(serv_adr)) == -1) {
		error_handling("connect() error");
	}
	else {
		puts("");
		puts("======== MENU ========");
		puts("[0] 서비스 종료");
		puts("[1] 로그인");
		puts("[2] 회원가입");
		puts("[3] 전체 프로그램 조회");
		puts("[4] 수강 신청 조회");
		puts("======================");
		puts("");
	}

	while (1) {
		fputs(">> ", stdout);
		fgets(message, BUF_SIZE, stdin);

		if (!strcmp(message, "0\n"))
			break;
		}

		write(sock, message, strlen(message));
		str_len = read(sock, message, BUF_SIZE - 1);
		message[str_len] = 0;
		printf("SYSTEM : %s ", message);
	}
	close(sock);
	return 0;
}

void error_handling(char* message) {
	fputs(message, stderr);
	fputc('\n', stderr);
	exit(1);
}