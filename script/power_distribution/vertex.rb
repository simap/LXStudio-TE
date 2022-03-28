require 'csv'

class Vertex
  def initialize(id:, x:, y:, z:)
    @id = id
    @x = x
    @y = y
    @z = z
  end

  attr_accessor :id, :x, :y, :z

  def distance(other)
    Math.sqrt((x - other.x)**2 + (y - other.y)**2 + (z - other.z)**2)
  end

  def self.load_vertices(filename)
    rows = CSV.read(filename, col_sep: "\t")
    vertices = {}
    rows.each do |row|
      vertices[row[0].to_i] = new(id: row[0].to_i, x: row[1].to_i, y: row[2].to_i, z: row[3].to_i)
    end

    vertices
  end
end
