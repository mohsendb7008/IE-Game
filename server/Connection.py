import socket
from threading import Thread
import json
import time

import Classes

games = {}
global_game_id = 1
start_coordinates = [(12, 6), (0, 6), (6, 0), (6, 12)]


def thread_function(conn: socket, addr):
    global global_game_id
    with conn:
        print('Connected by', addr)
        data = conn.recv(1024)
        str_data = json.loads(data.decode())
        print(str_data)
        type_ = str_data['type']
        name = str_data['name']
        if type_ == 'JOIN':
            id_ = str_data['id']
            print("{} with name {} joined to game {}".format(addr, name, id_))
            if id_ in games.keys():
                games[id_].append((name, conn))
            conn.send(b'{"type": "ACK"}\n')
        elif type_ == 'CREATE':
            game_id = global_game_id
            print("{} with name {} created a game with id {}".format(addr, name, game_id))
            res = {"id": game_id}
            global_game_id += 1
            conn.send((json.dumps(res) + "\n").encode())
            games[game_id] = [(name, conn)]
            while len(games[game_id]) != 4:
                time.sleep(1)

            players = []
            for i, (name, conn) in enumerate(games[game_id]):
                players.append(
                    Classes.Player(i, conn, name, i // 2, 1 - i if i < 2 else 5 - i, Classes.Point(start_coordinates[i][0], start_coordinates[i][1])))

            for i, (name, conn) in enumerate(games[game_id]):
                res = {
                    "type": "START",
                    "players": []
                }
                if i == 0:
                    res['players'] = [player.serialize() for player in players]
                elif i == 1:
                    res['players'] = [player.serialize() for player in [players[1], players[0]] + players[2:]]
                elif i == 2:
                    res['players'] = [player.serialize() for player in players[2:] + players[:2]]
                else:
                    res['players'] = [player.serialize() for player in [players[3], players[2]]+players[:2]]
                print(res)
                conn.send((json.dumps(res) + '\n').encode())
            print("Sent list of players to all")
            conn_players = []
            for i, p in enumerate(players):
                conn_players.append((p, games[game_id][i][1]))
            game = Classes.Game(conn_players)

            th = Thread(target=game.run, args=())
            th.start()

        while True:
            time.sleep(1)


if __name__ == "__main__":
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(('192.168.43.171', 5002))
        sock.listen()
        while True:
            conn, addr = sock.accept()
            t = Thread(target=thread_function, args=(conn, addr))
            t.start()
