import json
from socket import timeout


class Point:
    def __init__(self, row, column):
        self._row = row
        self._column = column

    @property
    def row(self):
        return self._row

    @property
    def column(self):
        return self._column

    def serialize(self):
        return {"row": self._row, "column": self._column}


class Player:
    def __init__(self, id, socket, name, group, teammate, coordinate):
        self._id = id
        self._socket = socket
        self._name = name
        self._group = group
        self._teammate = teammate
        self._coordinate = coordinate

    @property
    def id(self):
        return self._id

    @property
    def socket(self):
        return self._socket

    @property
    def name(self):
        return self._name

    @property
    def group(self):
        return self._group

    @property
    def teammate(self):
        return self._teammate

    @property
    def coordinate(self):
        return self._coordinate

    @coordinate.setter
    def coordinate(self, coordinate):
        self._coordinate = coordinate

    def serialize(self):
        return {
            "id": self._id,
            "name": self._name,
            "group": self._group,
            "coordinate": self._coordinate.serialize(),
            "teammate": self._teammate,
        }


class Cell:
    def __init__(self, coordinate: Point):
        self._coordinate = coordinate
        self._player = None

    @property
    def coordinate(self):
        return self._coordinate

    @property
    def player(self):
        return self._player

    @player.setter
    def player(self, player):
        self._player = player

    def serialize(self):
        return {
            "coordinate": self._coordinate.serialize(),
            "player": self._player.serialize(),
        }


class Game:
    def __init__(self, players: list):
        self._height = 13
        self._width = 13
        self._cycle = 1
        self._turn = -1
        self._grid = []

        for i in range(self._height):
            for j in range(self._width):
                self._grid.append((Cell(Point(i, j))))

        self._players = players
        self._walls = {}

    @property
    def cycle(self):
        return self._cycle

    @property
    def turn(self):
        return self._turn

    @property
    def grid(self):
        return self._grid

    @property
    def players(self):
        return self._players

    @property
    def walls(self):
        return self._walls

    def adjacents(self, cell):
        adjs = []
        adjR = [0, 0, 1, -1]
        adjC = [1, -1, 0, 0]
        dir = ['R', 'L', 'D', 'U']
        for i in range(4):
            if cell.coordinate in self._walls.keys():
                if dir[i] in self._walls[cell.coordinate]:
                    continue
            new_cell = Cell(Point(cell.coordinate.row + adjR[i], cell.coordinate.column + adjC[i]))
            if 0 <= new_cell.coordinate.row < self._height and 0 <= new_cell.coordinate.column < self._width:
                adjs.append((dir[i], new_cell))
        return adjs

    @staticmethod
    def index(row, column):
        return row*13+column

    def run(self):
        win = False
        if self._cycle == 1:
            res = {
                "type": "NOP",
                "turn": 0
            }
            for p, conn in self._players:
                conn.send((json.dumps(res) + '\n').encode())
                print(res)
            print("Sent NOP to all for the first time")

        else:
            l = [0, 2, 1, 3]
            player, conn = self._players[l[self._turn]]
            res = {"player": player.id}
            conn.settimeout(20)
            try:
                data = conn.recv(1024)
                str_data = json.loads(data.decode())
                type_ = str_data['type']
                print(str_data)
                res['turn'] = l[(self._turn + 1) % 4]

                if type_ == 'MOVE':
                    res['type'] = type_
                    action = str_data['action']
                    res['action'] = action
                    player_cell = self._grid[Game.index(player.coordinate.row, player.coordinate.column)]
                    adjs = self.adjacents(player_cell)
                    print(adjs)
                    for act, cell in adjs:
                        print(act, action)
                        if action == act:
                            self._grid[self.index(player.coordinate.row, player.coordinate.column)].player = None
                            player.coordinate = cell.coordinate
                            self._grid[self.index(cell.coordinate.row, cell.coordinate.column)] = player
                            print(player.coordinate.row, player.coordinate.column)
                            if player.coordinate.row == 6 and player.coordinate.column == 6:
                                res['type'] = "WIN"
                                res['group'] = player.group
                                win = True

                elif type_ == 'BLOCK':
                    res['type'] = type_
                    point1 = Point(str_data['point1']['row'], str_data['point1']['column'])
                    point2 = Point(str_data['point2']['row'], str_data['point2']['column'])
                    print(point1.row, point1.column, " ", point2.row, point2.column)

                    if point1 not in self._walls.keys():
                        self._walls[point1] = []
                    if point2 not in self._walls.keys():
                        self._walls[point2] = []

                    if point1.row == point2.row + 1:
                        self._walls[point1].append('U')
                        self._walls[point2].append('D')

                    elif point1.row == point2.row - 1:
                        self._walls[point1].append('D')
                        self._walls[point2].append('U')

                    elif point1.column == point2.column + 1:
                        self._walls[point1].append('L')
                        self._walls[point1].append('R')

                    elif point1.column == point2.column - 1:
                        self._walls[point1].append('R')
                        self._walls[point1].append('L')

                    res['point1'] = point1.serialize()
                    res['point2'] = point2.serialize()

                for p, conn in self._players:
                    conn.send((json.dumps(res) + '\n').encode())
                    print(json.dumps(res))

                if win:
                    return

            except:
                print("timeout")
                res = {
                    "type": "NOP",
                    "turn": l[(self._turn + 1) % 4],
                }
                for p, conn in self._players:
                    conn.send((json.dumps(res) + '\n').encode())

        self._turn = (self._turn + 1) % 4
        self._cycle += 1
        self.run()
