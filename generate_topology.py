# mypy: disable-error-code="import"

import abc
import random

from collections import defaultdict, deque
from dataclasses import dataclass, field
from functools import partial
from itertools import chain, count, combinations
from math import sin, cos, pi
from typing import (
    Any,
    ClassVar,
    Deque,
    Dict,
    Iterable,
    List,
    MutableSet,
    Optional,
    Tuple,
)


def check_ip_str(ip_str: str, subnet: bool = True):
    if subnet:
        ip_parts = ip_str.split('/')
        if len(ip_parts) != 2:
            raise ValueError('IP address of the host not found. '
                             'Must be of the form X.X.X.X/X')
        ip, mask = ip_parts
        if not 0 < int(mask) <= 32:
            raise ValueError(f'/{mask} is not a valid subnet mask')
    else:
        ip = ip_str

    ip_parts = ip.split('.')

    if len(ip_parts) != 4:
        raise ValueError(f'{ip} is not a valid IP address')

    for part in ip_parts:
        ipart = int(part)
        if ipart < 0:
            raise ValueError(f'{ip} is not a valid IP address. '
                             'Cannot have negative numbers.')
        if ipart > 256:
            raise ValueError(f'{ip} is not a valid IP address. '
                             'Cannot have value greater than 256.')


@dataclass
class Interface:
    ip: str
    mask: str = '24'

    def __str__(self):
        return f'{self.ip}/{self.mask}'


@dataclass
class Node(abc.ABC):
    name: str

    def __hash__(self):
        return hash(self.name)


@dataclass
class Router(Node):
    hosts: Dict['Host', Interface] = field(default_factory=dict)
    interfaces: List[Interface] = field(default_factory=list)

    ID: int = field(default_factory=partial(
        next, count(start=1)))  # type:ignore

    def __str__(self):
        ifaces = " ".join(map(str, self.interfaces))
        return f'router {self.name} {ifaces}'

    def __hash__(self):
        return super().__hash__()

    @classmethod
    def from_str(cls, line: str) -> 'Router':
        parts = line.strip().split()

        if len(parts) < 3 or parts[0] != 'router':
            raise ValueError('Invalid router declaration')

        interfaces = list()

        for ip_str in parts[2:]:
            check_ip_str(ip_str)
            ip, mask = ip_str.split('/')
            interfaces.append(Interface(ip, mask=mask))

        return cls(parts[1], interfaces=interfaces)


@dataclass
class Host(Node):
    ip: str
    gateway: Router
    mask: str = '24'

    def __str__(self):
        return f'host {self.name} {self.ip}/24 {self.gateway.hosts[self].ip}'

    def __hash__(self):
        return super().__hash__()

    @classmethod
    def from_str(cls, line: str, routers: Iterable[Router]) -> 'Host':
        parts = line.strip().split()

        if len(parts) != 4 or parts[0] != 'host':
            raise ValueError('Invalid host declaration')

        check_ip_str(parts[2])
        check_ip_str(parts[3], subnet=False)

        done = False
        for router in routers:
            for iface in router.interfaces:
                if iface.ip == parts[3]:
                    gateway = router
                    done = True
                    break
            if done:
                break
        else:
            raise ValueError('Could not find gateway router with interface '
                             f'{parts[3]}')

        host = cls(parts[1], parts[2], gateway)
        gateway.hosts[host] = iface

        return host


@dataclass
class Link:
    node1: Node
    node2: Node

    links_to_host: bool = False

    taken_subnets: ClassVar[MutableSet[int]] = set()

    def __post_init__(self):
        hosts = tuple(filter(lambda x: isinstance(
            x, Host), [self.node1, self.node2]))
        if len(hosts) == 2:
            raise ValueError(
                f'Cannot link two hosts {self.node1} and {self.node2}')

        if hosts:
            subnet = hosts[0].ip.split('.')[-2]
            Link.taken_subnets.add(subnet)
            self.links_to_host = True
            return

        for interface in chain(self.node1.interfaces, self.node2.interfaces):
            subnet = interface.ip.split('.')[-2]
            Link.taken_subnets.add(subnet)

        if len(Link.taken_subnets) > 254:
            raise ValueError('All subnets taken')

        all_subnets = map(str, range(1, 255))
        choices = filter(lambda x: x not in Link.taken_subnets, all_subnets)

        subnet = random.choice(tuple(choices))

        Link.taken_subnets.add(subnet)
        for node in [self.node1, self.node2]:
            iface = Interface(f'10.0.{subnet}.{node.ID}')
            node.interfaces.append(iface)

    def __contains__(self, obj: Any) -> bool:
        if not isinstance(obj, Node):
            return False

        return self.node1 == obj or self.node2 == obj

    def __hash__(self):
        return hash(f'{self.node1.name} {self.node2.name}')

    def __str__(self):
        return f'link {self.node1.name} {self.node2.name}'

    @classmethod
    def from_str(cls, line: str, nodes: Dict[str, Node]) -> 'Link':
        parts = line.strip().split()

        if parts[0] != 'link' or len(parts) != 3:
            raise ValueError('Invalid link declaration.')

        for part in parts[1:]:
            if part not in nodes:
                raise ValueError(f'{part} is not a known host or router')

        return cls(nodes[parts[1]], nodes[parts[2]])

    def peer(self, node: Node):
        if node == self.node1:
            return self.node2
        elif node == self.node2:
            return self.node1
        else:
            return None


@dataclass(init=False, repr=True)
class Topo:
    hosts: List[Host]
    n_hosts: int
    routers: List[Router]
    n_routers: int
    links: List[Link]
    n_links: int

    def __init__(self, hosts: Iterable[Host], routers: Iterable[Router],
                 links: Iterable[Link]):
        self.hosts: List[Host] = list(hosts)
        self.routers: List[Router] = list(routers)
        self.links: List[Link] = list(links)
        self.n_hosts = len(self.hosts)
        self.n_routers = len(self.routers)
        self.n_links = len(self.links)

    def __str__(self):
        hosts = '\n'.join(sorted(map(str, topo.hosts)))
        routers = '\n'.join(sorted(map(str, topo.routers)))
        links = '\n'.join(sorted(map(str, topo.links)))

        return '\n'.join((hosts, routers, links))

    @classmethod
    def load(cls, filename: str) -> 'Topo':
        host_lines, hosts = list(), list()
        router_lines, routers = list(), list()
        link_lines, links = list(), list()

        with open(filename, 'r') as f:
            for linenum, line in enumerate(f, start=1):
                line = line.strip()

                if line.startswith('host'):
                    host_lines.append((linenum, line))
                elif line.startswith('router'):
                    router_lines.append((linenum, line))
                elif line.startswith('link'):
                    link_lines.append((linenum, line))
                else:
                    raise ValueError(f'Error on line {linenum}:\n'
                                     f'  Cannot parse "{line}".')

        nodes: Dict[str, Node] = {}

        for linenum, line in router_lines:
            try:
                router = Router.from_str(line)
            except ValueError as e:
                raise ValueError(f'Error on line {linenum}:\n  {e}')
            routers.append(router)
            nodes[router.name] = router

        for linenum, line in host_lines:
            try:
                host = Host.from_str(line, routers)
            except ValueError as e:
                raise ValueError(f'Error on line {linenum}:\n  {e}')
            hosts.append(host)
            nodes[host.name] = host

        for linenum, line in link_lines:
            try:
                link = Link.from_str(line, nodes)
            except ValueError as e:
                raise ValueError(f'Error on line {linenum}:\n  {e}')
            links.append(link)

        return cls(hosts, routers, links)

    def save(self, filename: str):
        with open(filename, 'w') as f:
            f.write(str(self))

    def find_subgraphs(self) -> MutableSet[Tuple[Router, ...]]:
        edges: Dict[Router, List[Router]] = defaultdict(list)
        router_links = filter(lambda x: not x.links_to_host, self.links)
        for link in router_links:
            edges[link.node1].append(link.node2)  # type:ignore[index,arg-type]
            edges[link.node2].append(link.node1)  # type:ignore[index,arg-type]

        graphs = set()

        unconnected_routers = set(self.routers)

        while unconnected_routers:
            start = unconnected_routers.pop()

            queue: Deque[Router] = deque([start])
            visited: MutableSet[Router] = set([start])

            while queue:
                curr = queue.pop()
                for node in edges[curr]:
                    if node not in visited:
                        queue.append(node)
                        visited.add(node)

            unconnected_routers = unconnected_routers - visited
            graphs.add(tuple(visited))

        return graphs

    def ensure_connected(self):
        subgraphs = self.find_subgraphs()

        if not subgraphs:
            return

        subgraphs = tuple(subgraphs)

        for graph1, graph2 in zip(subgraphs, subgraphs[1:]):
            node1 = random.choice(graph1)
            node2 = random.choice(graph2)
            self.links.append(Link(node1, node2))

    def visualize(self, block=True):
        try:
            from matplotlib import pyplot as plt
        except ImportError:
            raise NotImplementedError(
                'matplotlib is required for visualization.'
            )

        # random.seed(self.n_hosts + self.n_links + self.n_routers)

        vertices = {}
        n = self.n_routers
        two_pi_over_n = 2 * pi / n
        for i, node in enumerate(self.routers):
            arg = i * two_pi_over_n
            point = 10 * cos(arg), 10 * sin(arg)
            vertices[node.name] = point

        for i, node in enumerate(self.hosts):
            arg = i * two_pi_over_n
            point = 15 * cos(arg), 15 * sin(arg)
            vertices[node.name] = point

        fig, ax = plt.subplots(nrows=1, ncols=1)

        for name, point in vertices.items():
            ax.text(*point, s=name, mouseover=True, ha='center', va='center',
                    bbox=dict(facecolor='blue' if name[0] == 'r' else 'green',
                              edgecolor='black', boxstyle='circle'))

        for edge in self.links:
            node1, node2 = edge.node1.name, edge.node2.name
            xs = [vertices[node1][0], vertices[node2][0]]
            ys = [vertices[node1][1], vertices[node2][1]]
            ax.plot(xs, ys, c='k')

            ax.axis('off')

        plt.show(block=block)


def gen_topo(n_hosts: Optional[int] = None,
             n_routers: Optional[int] = None,
             density: float = 0.4) -> Topo:

    if n_hosts is None:
        n_hosts = random.randint(2, 6)

    if n_routers is None:
        n_routers = random.randint(4, 8)

    routers = [Router(f'r{i + 1}') for i in range(n_routers)]

    # host_routers = routers[:n_hosts]
    host_routers = random.choices(routers, k=n_hosts)

    hosts: List[Host] = list()
    links: MutableSet[Link] = set()

    for i, router in enumerate(host_routers, start=1):
        ip = f'10.0.{i}.{100 + i}'
        gwip = f'10.0.{i}.{router.ID}'

        iface = Interface(gwip)
        router.interfaces.append(iface)

        host = Host(f'h{i}', ip, router)
        hosts.append(host)

        router.hosts[host] = iface

        links.add(Link(host, router))

    if not (0 < density <= 1):
        raise ValueError('Density must be a fractional value between 0 and 1')

    for edge in combinations(routers, 2):
        if random.random() <= density:
            links.add(Link(*edge))

    topo = Topo(hosts, routers, links)
    # topo.ensure_connected()
    return topo


if __name__ == '__main__':
    topo = gen_topo(n_hosts=0, n_routers=10, density=.1)
    # topo.visualize()
    topo.ensure_connected()
    # topo = Topo.load('P3/topos/cursed.topo')
    print('Hosts', topo.n_hosts)
    print(*topo.hosts, sep='\n')
    print('\nRouters', topo.n_routers)
    print(*topo.routers, sep='\n')
    print('\nLinks', topo.n_links)
    print(*topo.links, sep='\n')
    # topo.save('auto_gen.topo')
    topo.visualize()
