#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

// Вспомогательная функция для отправки HCI-команды
int send_hci_command(int dev_id, uint16_t ogf, uint16_t ocf, uint8_t *param, size_t param_len) {
    int sock = hci_open_dev(dev_id);
    if (sock < 0) {
        perror("hci_open_dev");
        return -1;
    }

    struct hci_request req;
    memset(&req, 0, sizeof(req));
    req.ogf = ogf;
    req.ocf = ocf;
    req.cparam = param;
    req.clen = param_len;
    req.rparam = NULL;
    req.rlen = 0;

    int ret = hci_send_req(sock, &req, 1000);
    hci_close_dev(sock);
    return ret;
}

int main(int argc, char *argv[]) {
    if (argc < 4) {
        fprintf(stderr, "Usage: %s <dev_id> <ogf> <ocf> [hex_params]\n", argv[0]);
        return 1;
    }

    int dev_id = atoi(argv[1]);
    uint16_t ogf = strtol(argv[2], NULL, 16);
    uint16_t ocf = strtol(argv[3], NULL, 16);

    uint8_t params[256];
    size_t param_len = 0;

    if (argc > 4) {
        char *hex = argv[4];
        size_t hex_len = strlen(hex);
        if (hex_len % 2 != 0) {
            fprintf(stderr, "Hex string must have even length\n");
            return 1;
        }
        param_len = hex_len / 2;
        for (size_t i = 0; i < param_len; i++) {
            sscanf(hex + i * 2, "%2hhx", &params[i]);
        }
    }

    if (send_hci_command(dev_id, ogf, ocf, params, param_len) < 0) {
        fprintf(stderr, "Failed to send HCI command\n");
        return 1;
    }

    return 0;
}
