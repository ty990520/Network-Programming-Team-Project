#include <sys/socket.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>

#define BUF_SIZE 1024
int loginFlag = 0;
void error_handling(char* message);
void printMenu();

int main(int argc, char* argv[]) {
    int sock;
    char message[BUF_SIZE];
    int str_len = 0;
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
        printMenu();
    }

    while (1) {
        fputs(">> ", stdout);
        fgets(message, BUF_SIZE, stdin);

        if (!strcmp(message, "0\n"))
            break;

        write(sock, message, strlen(message));
        str_len = read(sock, message, BUF_SIZE - 1);
        message[str_len] = 0;
        printf(message);

        char subtext[6];
        strncpy(subtext, &message[1], 5);
        subtext[5] = '\0';

        if (strcmp(subtext, "LOGIN") == 0) {
            loginFlag = 1;
        }

        if (strcmp(subtext, "LOGOU") == 0) {
            loginFlag = 0;
        }

        if (strcmp(subtext, "LOGIN") == 0 || strcmp(subtext, "SUCCE") == 0 || strcmp(subtext, "LOGOU") == 0) {
            printMenu();
        }
    }
    close(sock);
    return 0;
}

void printMenu() {
    puts("");
    puts("======== MENU ========");
    puts("[0] 서비스 종료");
    if (loginFlag == 1) {
        puts("[1] 로그아웃");
    }
    else {
        puts("[1] 로그인");
    }
    puts("[2] 회원가입");
    puts("[3] 전체 프로그램 조회");
    if (loginFlag == 1) {
        puts("[4] 수강 신청 조회");
    }
    puts("======================");
    puts("");
}

void error_handling(char* message) {
    fputs(message, stderr);
    fputc('\n', stderr);
    exit(1);
}