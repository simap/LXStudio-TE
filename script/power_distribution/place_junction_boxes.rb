require 'csv'
require 'pp'

require './vertex.rb'
require './edge.rb'
require './graph.rb'
require './panel.rb'

class JunctionBox
  def initialize(vertex:)
    @vertex = vertex
    @id = vertex.id
    @circuits = (0..15).map { |i| JunctionBoxCircuit.new(id: "#{id}-#{i}", junction_box_id: id) }
  end

  def current
    circuits.sum(&:current)
  end

  def utilization
    circuits.sum(&:utilization) / circuits.size
  end

  def dup
    b = JunctionBox.new(vertex: @vertex)
    b.circuits = circuits.map(&:dup)
    b
  end

  attr_accessor :id, :circuits
end

class JunctionBoxCircuit
  MAX_CURRENT = 15
  def initialize(id:, junction_box_id:)
    @id = id
    @junction_box_id = junction_box_id
    @panel_strips = []
    @edge_strips = []
  end

  attr_accessor :panel_strips, :edge_strips, :id, :junction_box_id

  def dup
    c = JunctionBoxCircuit.new(id: id, junction_box_id: junction_box_id)
    c.panel_strips = panel_strips.dup
    c.edge_strips = edge_strips.dup
    c
  end

  def current
    panel_strips.sum(&:current) + edge_strips.sum(&:max_current)
  end

  def utilization
    current / MAX_CURRENT
  end
end

def load_edge_paths(filename, edges, vertices)
  rows = CSV.read(filename, col_sep: "\t")
  edge_paths = [[]]
  rows.each do |row|
    edge = edges[row[0]]
    next if edge.nil?

    signal_from = row[16]
    if signal_from == 'Controller'
      edge.signal_from = vertices[row[17].to_i]
    else
      prev_edge = edges[row[16]]
      edge.signal_from = prev_edge
      prev_edge.signal_to = edge
    end

    edge_paths.last << edge
    if row[18] == 'Terminates'
      edge_paths << []
    end
  end
  edge_paths
end

# Constraints:
#
#   1. Each junction box will contain 4 RSP-320-5 power supplies, for a total of
#   240A of current
#
#   2. Each power supply will be divided into 4 15A fused circuits
#
#   3. [TODO] Placement must be symmetrical where possible
def place_junction_boxes(graph:, panels:)
  junction_boxes = {}

  graph.edges.each_value do |edge|
    edge.strips.each do |strip|
      candidates = edge_strip_assignment_candidates(
        strip: strip,
        graph: graph,
        junction_boxes: junction_boxes,
      )
      if candidates.empty?
        vertex = strip.vertices[0]

        box = JunctionBox.new(vertex: vertex)
        box.circuits[0].edge_strips << strip

        junction_boxes[vertex.id] ||= []
        junction_boxes[vertex.id] << box
        next
      end

      circuit = candidates.min_by(&:utilization)
      circuit.edge_strips << strip
    end
  end

  panels.each_value do |panel|
    panel.strips.each do |strip|
      candidates = panel_strip_assignment_candidates(strip: strip, graph: graph, junction_boxes: junction_boxes)
      if candidates.empty?
        vertex = panel.vertices[0]
        box = JunctionBox.new(vertex: vertex)
        box.circuits[0].panel_strips << strip
        puts vertex.id
        junction_boxes[vertex.id] ||= []
        junction_boxes[vertex.id] << box
        next
      end

      circuit = candidates.min_by(&:utilization)
      circuit.panel_strips << strip
    end
  end
  junction_boxes
end

def edge_strip_assignment_candidates(strip:, graph:, junction_boxes:)
  # Consider junction boxes that already exist at each of the vertices of the
  # edge as well as junction boxes placed at immediate neigbors of those
  # vertices if the distance from that neighbor to each end of the edge is <=
  # 17 ft (1V drop for 12 AWG). Pick the candidate that leads to the best
  # average utilization, or create a new box if one doesn't already exist.
  candidate_circuits = []

  strip.vertices.each do |vertex|
    if !junction_boxes[vertex.id].nil?
      candidate_circuits += junction_boxes[vertex.id]
        .map(&:circuits)
        .flatten
        .select { |c| c.current + strip.max_current <= JunctionBoxCircuit::MAX_CURRENT }
    end
  end

  if candidate_circuits.empty?
    vertices = strip.vertices
      .map { |v| graph.adjacency[v.id] }
      .flatten
      .uniq
      .select { |v| strip.vertices.all? { |w| graph.min_distance(v, w.id) < 17 * 304_800} }
    vertices.each do |v|
      if !junction_boxes[v].nil?
        candidate_circuits += junction_boxes[v]
          .map(&:circuits)
          .flatten
          .select { |c| c.current + strip.max_current <= JunctionBoxCircuit::MAX_CURRENT }
      end
    end
  end

  candidate_circuits
end

def balance_junction_boxes(junction_boxes:, graph:)
  boxes = junction_boxes
  loop do
    changed = false

    junction_boxes.values.flatten.sort_by(&:utilization).each do |box|
      new_boxes = duplicate_junction_boxes(boxes)
      success = try_reassign_box(box: box, boxes: new_boxes, graph: graph)
      if success
        puts "success"
        delete_box!(box: box, boxes: new_boxes)
        boxes = new_boxes
        changed = true
      end
    end

    return boxes unless changed
  end
end

def try_reassign_box(box:, boxes:, graph:)
  box.circuits.each do |circuit|
    circuit.edge_strips.each do |strip|
      candidates = edge_strip_assignment_candidates(strip: strip, graph: graph, junction_boxes: boxes)
      new_circuit = candidates
        .select { |c| c.junction_box_id != box.id }
        .max_by(&:utilization)

      return false if new_circuit.nil?

      new_circuit.edge_strips << strip
    end

    circuit.panel_strips.each do |strip|
      candidates = panel_strip_assignment_candidates(strip: strip, graph: graph, junction_boxes: boxes)
      new_circuit = candidates
        .select { |c| c.junction_box_id != box.id }
        .max_by(&:utilization)

      return false if new_circuit.nil?

      new_circuit.panel_strips << strip
    end
  end
end

def duplicate_junction_boxes(boxes)
  new_boxes = {}
  boxes.each do |vertex, vertex_boxes|
    new_boxes[vertex] = []
    vertex_boxes.each do |box|
      new_boxes[vertex] << box.dup
    end
  end
  new_boxes
end

def delete_box!(box:, boxes:)
  boxes.each do |vertex, vertex_boxes|
    boxes[vertex] = vertex_boxes.select { |b| b.id != box.id }
  end
end

def panel_strip_assignment_candidates(strip:, graph:, junction_boxes:)
  candidate_circuits = []

  strip.vertices.each do |vertex|
    if !junction_boxes[vertex.id].nil?
      debug_candidates = junction_boxes[vertex.id]
        .map(&:circuits)
        .flatten

      candidate_circuits += junction_boxes[vertex.id]
        .map(&:circuits)
        .flatten
        .select { |c| c.current + strip.current <= JunctionBoxCircuit::MAX_CURRENT }
    end
  end

  if candidate_circuits.empty?
    vertices = strip.vertices
      .map { |v| graph.adjacency[v.id] }
      .flatten
      .uniq
      .select { |v| strip.vertices.all? { |w| graph.min_distance(v, w.id) < (17 * 304_800) } }
    vertices.each do |v|
      if !junction_boxes[v].nil?
        candidate_circuits += junction_boxes[v]
          .map(&:circuits)
          .flatten
          .select { |c| c.current + strip.current <= JunctionBoxCircuit::MAX_CURRENT }
      end
    end
  end

  candidate_circuits
end

def print_boxes(junction_boxes)
  junction_boxes.each do |vertex, boxes|
    next if boxes.empty?
    puts "#{vertex} - #{boxes.sum(&:current)} A - #{100 * (boxes.sum(&:utilization) / boxes.size).truncate(4)}% utilized"
  end

  boxes = junction_boxes.values.flatten
  puts junction_boxes.keys.length
  puts "#{junction_boxes.values.flatten.size} total junction boxes"
  puts "#{boxes.sum(&:current)} Amps"
  puts "#{boxes.sum(&:utilization) / boxes.count} average utilization"
end

vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges)
boxes = place_junction_boxes(graph: graph, panels: panels)
print_boxes(boxes)
boxes = balance_junction_boxes(junction_boxes: boxes, graph: graph)
print_boxes(boxes)
