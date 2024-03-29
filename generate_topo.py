from dataclasses import dataclass, field
from matplotlib import pyplot as plt
from typing import Optional, ClassVar, Iterable
import abc
import itertools
import random


plt.style.use('dark_background')


@dataclass
class Node(abc.ABC):
    name: str


@dataclass(order=True)
class Router(Node):
    interfaces: list = field(default_factory=list)

    ID: int = -1
    _id: ClassVar[int] = 1

    def __post_init__(self):
        self.ID = Router._id
        Router._id += 1

    def __hash__(self):
        return hash(self.name)

    def __str__(self):
        ifaces = map(lambda x: f'{x}/24', self.interfaces)
        return f'router {self.name} {" ".join(ifaces)}'


@dataclass
class Host(Node):
    ip: str
    gateway: Router

    ID: int = -1
    _id: ClassVar[int] = 1

    def __post_init__(self):
        self.ID = Host._id
        Host._id += 1

    def __hash__(self):
        return hash(self.name)

    def __str__(self):
        return f'host {self.name} {self.ip}/24 {self.gateway.interfaces[0]}'


@dataclass
class Link:
    node1: Host | Router
    node2: Host | Router

    taken_subnets: ClassVar[set[int]] = set()

    def __post_init__(self):
        hosts = tuple(filter(lambda x: isinstance(
            x, Host), [self.node1, self.node2]))
        if len(hosts) == 2:
            raise ValueError(
                f'Cannot link two hosts {self.node1} and {self.node2}')

        if hosts:
            subnet = hosts[0].ip.split('.')[-2]
            Link.taken_subnets.add(subnet)
            return

        for interface in itertools.chain(self.node1.interfaces, self.node2.interfaces):
            subnet = interface.split('.')[-2]
            Link.taken_subnets.add(subnet)

        if len(Link.taken_subnets) > 254:
            raise ValueError('All subnets taken')

        all_subnets = map(str, range(1, 255))
        choices = filter(lambda x: x not in Link.taken_subnets, all_subnets)

        subnet = random.choice(tuple(choices))

        Link.taken_subnets.add(subnet)
        for node in [self.node1, self.node2]:
            node.interfaces.append(f'10.0.{subnet}.{node.ID}')

    def __hash__(self):
        return hash(f'{self.node1.name}_{self.node2.name}')

    def __str__(self):
        return f'link {self.node1.name} {self.node2.name}'


@dataclass(init=False, repr=True)
class Topo:
    hosts: list[Host]
    routers: list[Router]
    links: list[Link]
    n_hosts: int
    n_routers: int
    n_links: int

    def __init__(self, hosts: Iterable[Host], routers: Iterable[Router],
                 links: Iterable[Link]):
        self.hosts: list[Host] = list(hosts)
        self.routers: list[Router] = list(routers)
        self.links: list[Link] = list(links)
        self.n_hosts = len(self.hosts)
        self.n_routers = len(self.routers)
        self.n_links = len(self.links)


def rand_topo(n_hosts: Optional[int] = None,
              n_routers: Optional[int] = None) -> Topo:

    if n_hosts is None:
        n_hosts = random.randint(2, 6)

    if n_routers is None:
        n_routers = random.randint(4, 8)

    routers = list(Router(f'r{i + 1}') for i in range(n_routers))

    host_routers = routers[:n_hosts]

    hosts: list[Host] = list()
    links: set[Link] = set()

    for i, router in enumerate(host_routers, start=1):
        ip = f'10.0.{i}.{100 + i}'
        gwip = f'10.0.{i}.{router.ID}'
        router.interfaces.append(gwip)
        hosts.append(Host(f'h{i}', ip, router))
        links.add(Link(hosts[-1], router))

    try:
        n_links = min(15, random.randint(
            n_routers, (n_routers * (n_routers - 1)) // 2))
    except ValueError:
        print(f'{n_routers = }')
        raise
    while len(links) != n_links:
        linked_routers = random.sample(routers, k=2)
        linked_routers.sort()
        link = Link(*linked_routers)
        links.add(link)

    return Topo(hosts, routers, links)


def check_topo(topo: Topo, verbose: bool = True) -> bool:
    for router in topo.routers:
        if len(router.interfaces) < 2:
            if verbose:
                print('At least one router with no interfaces')
            return False

    return True


def gen_topo(topo) -> str:
    if not check_topo(topo):
        return ''

    hosts = '\n'.join(sorted(map(str, topo.hosts)))
    routers = '\n'.join(sorted(map(str, topo.routers)))
    links = '\n'.join(sorted(map(str, topo.links)))

    return '\n'.join((hosts, routers, links))


while True:
    print('.', end='', flush=True)
    topo = rand_topo(n_hosts=4, n_routers=6)

    if check_topo(topo, verbose=False):
        break
print()
points = set()

vertices = {}
for node in itertools.chain(topo.hosts, topo.routers):
    while True:
        point = (random.randint(1, 10), random.randint(1, 10))
        if point not in points:
            points.add(point)
            break

    vertices[node.name] = point


fig, ax = plt.subplots(nrows=1, ncols=1)

for name, point in vertices.items():
    ax.text(*point, s=name, mouseover=True, ha='center', va='center', bbox=dict(
        facecolor='blue' if name[0] == 'h' else 'green', edgecolor='black',
        boxstyle='circle'))


for edge in topo.links:
    node1, node2 = edge.node1.name, edge.node2.name
    xs = [vertices[node1][0], vertices[node2][0]]
    ys = [vertices[node1][1], vertices[node2][1]]
    ax.plot(xs, ys, c='w')


print('-' * 80)
print(gen_topo(topo))

ax.axis('off')
plt.show()
