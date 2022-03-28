require 'csv'
require './constants.rb'

class Edge
  # 60 / m LED strips
  LEDS_PER_MICRON = 0.00006

  STRIPS_PER_EDGE = 3

  def initialize(id:, vertices:)
    @id = id
    @vertices = vertices
    @signal_to = nil
    @signal_from = nil
    @strips = Array.new(STRIPS_PER_EDGE) { EdgeStrip.new(vertices: vertices) }
  end

  def length
    @length ||= vertices[1].distance(vertices[0])
  end

  def num_leds
    @num_leds ||= strips.sum(&:num_leds)
  end

  def max_current
    @max_current ||= strips.sum(&:max_current)
  end

  def self.load_edges(filename, vertices)
    rows = CSV.read(filename, col_sep: "\t")
    edges = {}
    rows.each do |row|
      vs = row[0].split('-').map { |v| vertices[v.to_i] }
      edges[row[0]] = Edge.new(id: row[0], vertices: vs)
    end
    edges
  end

  attr_accessor :id, :vertices, :signal_to, :signal_from, :strips
end

class EdgeStrip
  def initialize(vertices:)
    @vertices = vertices
  end

  def num_leds
    @num_leds ||= length * Edge::LEDS_PER_MICRON
  end

  def max_current
    @max_current ||= num_leds * MAX_CURRENT_PER_LED
  end

  def length
    @length ||= vertices[1].distance(vertices[0])
  end

  attr_accessor :vertices
end

