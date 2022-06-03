#define _WINSOCK_DEPRECATED_NO_WARNINGS // 최신 VC++ 컴파일 시 경고 방지
#define _CRT_SECURE_NO_WARNINGS
#pragma warning(disable:4996)
#pragma comment(lib, "ws2_32")
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <winsock2.h>
#include <locale.h>

#define BUF_SIZE 1024
int loginFlag = 0;
void ErrorHandling(char* message);
void printMenu();


int main(int argc, char* argv[]) {
	_wsetlocale(LC_ALL, L"korean");
	WSADATA wsaData;
	SOCKET hSocket;
	char message[BUF_SIZE];
	int strLen;
	SOCKADDR_IN servAdr;
	


	if (argc != 3) {
		printf("Usage : %s [IP address] [Port]\n", argv[0]);
		exit(1);
	}

	if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0)
		ErrorHandling("WSAstartup() error");
	hSocket = socket(PF_INET, SOCK_STREAM, 0);
	if (hSocket == INVALID_SOCKET){
		ErrorHandling("socket() error : %d",WSAGetLastError());
	}

	memset(&servAdr, 0, sizeof(servAdr));
	servAdr.sin_family = AF_INET;
	servAdr.sin_addr.s_addr = inet_addr(argv[1]);
	servAdr.sin_port = htons(atoi(argv[2]));

	if (connect(hSocket, (SOCKADDR*)&servAdr, sizeof(servAdr)) == SOCKET_ERROR) {
		ErrorHandling("connect() error");
	}
	else {
		printMenu();
	}

	while (1) {
		fputs(">> ", stdout);
		fgets(message, BUF_SIZE, stdin);
		

		if (!strcmp(message, "0\n"))
			break;

		

		send(hSocket, message, strlen(message), 0);
		strLen = recv(hSocket, message, BUF_SIZE - 1, 0);
		message[strLen] = 0;
		
		
		printf(message);
		char subtext[6];
		strncpy(subtext, &message[1], 5);
		subtext[5] = '\0';

		if (strcmp(subtext, "LOGIN")==0) {
			loginFlag = 1;
		}
		if (strcmp(subtext, "LOGOU") == 0) {
			loginFlag = 0;
		}

		if (strcmp(subtext, "LOGIN") == 0 || strcmp(subtext, "SUCCE") == 0 || strcmp(subtext, "LOGOU") == 0) {
			printMenu();
		}
	}
	closesocket(hSocket);
	WSACleanup();
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
void ErrorHandling(char* message) {
	fputs(message, stderr);
	fputc('\n', stderr);
	exit(1);
}