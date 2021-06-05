#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <string.h>
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/extensions/XTest.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <xdo.h>

#define MOVE_C 1
#define LMOUSEDOWN_C 2
#define LMOUSEUP_C 3
#define LMOUSECLICK_C 4
#define RMOUSECLICK_C 5
#define SCROLLUP_C 6
#define SCROLLDOWN_C 7
#define CHARDOWN_C 8
#define KEYDOWN_C 9
#define HOME_C 10
#define VOLUME_C 11
#define POWEROFF 12
#define ACK 13
#define GOBACK_C 14
#define UNKNOWN_C -1
#define BUFFSIZE 24


void move (Display*, int, int);
void getCoords(Display*, int*, int*);
void click(Display*, int);
void mouseDown(Display*, int);
void mouseUp(Display*, int);
void charDown(xdo_t*, char*);
void keyDown(Display*, unsigned);
void goHome();
void adjustVolume(int);
void powerOff();
void goBack(xdo_t*);
int interpretCommand(char*);



/* main program */
/***************/
int main() {
    xdo_t *xdo = xdo_new(NULL);
    int x, y;
    int server_fd;
    int optval, read;
    struct sockaddr_in server, client;
    char buff[BUFFSIZE];
    char command[3];
    char* key;
    char ack[BUFFSIZE] = "ack\n";

    /*open display*/
    Display *display = XOpenDisplay(NULL);
    if (display == NULL) {
        fprintf(stderr, "Sorry, could not open display :[ \n");
        return -1;
    }

    /*create socket*/
    if ( (server_fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        fprintf(stderr, "Sorry, could not create a server socket :[ \n");
        return -1;
    }
    optval = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR,
        (const void *) &optval , sizeof(int));

    /*build address*/
    bzero((char *) &server, sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons(11111);
    server.sin_addr.s_addr = htonl(INADDR_ANY);

    /*bind socket to the chosen port*/
    if ( bind(server_fd, (struct sockaddr *) &server, sizeof(server)) < 0) {
        fprintf(stderr, "Sorry, port 11111 appears to be in use :[ \n");
        return -1;
    }


    /*listening remote control app*/
    socklen_t client_len = sizeof(client);
    while (1) {
        bzero(buff, BUFFSIZE);
        bzero(command, 3);
        read = recvfrom(server_fd, buff, BUFFSIZE, 0, (struct sockaddr *)&client, &client_len);
        if (!read) break;
        if (read < 0) {
            fprintf(stderr, "Sorry, there is an error with the connection...\n");
            break;
        }
        /*printf("server received %d bytes: %s\n", read, buff);*/
        sscanf(buff, "%s %s %d %d", command, key, &x, &y);
        switch (interpretCommand(command)) {
            case MOVE_C:        move(display, x, y); break;
            case LMOUSEDOWN_C:  mouseDown(display, Button1); break;
            case LMOUSEUP_C:    mouseUp(display, Button1); break;
            case LMOUSECLICK_C: click(display, Button1); break;
            case RMOUSECLICK_C: click(display, Button3); break;
            case SCROLLUP_C:    click(display, Button5); break;
            case SCROLLDOWN_C:  click(display, Button4); break;
            case CHARDOWN_C:    charDown(xdo, key); break;
            case KEYDOWN_C:     keyDown(display, (unsigned) x); break;
	        case HOME_C:        goHome(); break;
            case GOBACK_C:      goBack(xdo); break;
            case VOLUME_C:      adjustVolume(x); break;
	        case POWEROFF:      powerOff(); break;
	        case ACK:           sendto(server_fd, ack, BUFFSIZE, 0, 
				               (struct sockaddr*)&client, client_len); break;	
            default: break;
        }
    }

    XCloseDisplay(display);
    return 0;
}

/* move cursor to a (x,y) coordinate on screen */
/***********************************************/
void move (Display *display, int x, int y) {
    /* ABSOLUTE MOTION: XTestFakeMotionEvent(display, -1, x, y, CurrentTime);*/
    XTestFakeRelativeMotionEvent(display, x, y, CurrentTime);
    XFlush(display);
}


/* simulates a mouse click */
/*****************************/
void click(Display *display, int button) {
    XTestFakeButtonEvent(display, button, True, CurrentTime);
    XTestFakeButtonEvent(display, button, False, CurrentTime);
    XFlush(display);
}

/* simulates a mouse button press*/
/*********************************/
void mouseDown(Display *display, int button) {
    XTestFakeButtonEvent(display, button, True, CurrentTime);
    XFlush(display);
}

/* simulates a mouse button release*/
/***********************************/
void mouseUp(Display *display, int button) {
    XTestFakeButtonEvent(display, button, False, CurrentTime);
    XFlush(display);
}

/* simulates a key press event */
/*******************************/
void keyDown(Display *display, unsigned keycode) {
    XTestFakeKeyEvent(display, keycode, True, CurrentTime);
    XTestFakeKeyEvent(display, keycode, False, CurrentTime);
    XFlush(display);
}

/* simulates a string input */
/****************************/
void charDown(xdo_t* xdo, char* key) {
    //char* c = malloc(sizeof(char));
    //c = key;
    xdo_enter_text_window(xdo, CURRENTWINDOW, key, 0);
}

/* go to home screen */
/*********************/
void goHome() {
    pid_t pid;
    pid = fork();
    if (pid == 0) {
	system("killall brave");
	system("brave --kiosk /home/cadu/StreamCenter/welcome.html");
	exit(0);
    }
}

/* adjust system volume */
/**********************/
void adjustVolume(int value) {
    pid_t pid;
    pid = fork();
    if (pid == 0) { 
	char buf[22];
	snprintf(buf, 22, "pulseaudio-ctl set %d", value);
    	system(buf);
    }
}

/* turn off your machine */
/************************/
void powerOff() {
    pid_t pid;
    pid = fork();
    if (pid == 0) {
    	system("poweroff");
	exit(0);
    }
}

/*Go back in the browser*/
/************************/
void goBack(xdo_t* xdo) {
    xdo_send_keysequence_window(xdo, CURRENTWINDOW, "Alt_R+Left", 0);
}

/* interpret remote command*/
/***************************/
int interpretCommand(char *input) {
    if ( strcasecmp(input, "mov") == 0)
        return MOVE_C;
    if ( strcasecmp(input, "lmd") == 0)
        return LMOUSEDOWN_C;
    if ( strcasecmp(input, "lmu") == 0)
        return LMOUSEUP_C;
    if ( strcasecmp(input, "lmc") == 0)
        return LMOUSECLICK_C;
    if ( strcasecmp(input, "rmc") == 0)
        return RMOUSECLICK_C;
    if ( strcasecmp(input, "scu") == 0)
        return SCROLLUP_C;
    if ( strcasecmp(input, "scd") == 0)
        return SCROLLDOWN_C;
    if ( strcasecmp(input, "chr") == 0)
        return CHARDOWN_C;
    if ( strcasecmp(input, "key") == 0)
        return KEYDOWN_C;
    if ( strcasecmp(input, "hom") == 0)
	    return HOME_C;
    if ( strcasecmp(input, "vol") == 0)
    	return VOLUME_C;
    if ( strcasecmp(input, "pwr") == 0)
    	return POWEROFF;
    if ( strcasecmp(input, "ack") == 0)
	    return ACK;
    if ( strcasecmp(input, "gbk") == 0)
        return GOBACK_C;
    return UNKNOWN_C;
}
