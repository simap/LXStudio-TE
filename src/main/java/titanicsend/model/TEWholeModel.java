package titanicsend.model;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.lasercontrol.MovingTarget;
import titanicsend.output.TEArtNetOutput;
import titanicsend.util.TE;

public class TEWholeModel extends LXModel {
  public String subdir;
  public String name;
  public LXPoint gapPoint;  // Used for pixels that shouldn't actually be lit
  public HashMap<Integer, TEVertex> vertexesById;
  public HashMap<String, TEEdgeModel> edgesById;
  public HashMap<LXVector, List<TEEdgeModel>> edgesBySymmetryGroup;
  public HashMap<String, TEPanelModel> panelsById;
  private final HashMap<TEPanelSection, Set<TEPanelModel>> panelsBySection;
  public HashMap<String, List<TEPanelModel>> panelsByFlavor;
  public HashMap<String, TELaserModel> lasersById;
  public List<LXPoint> edgePoints; // Points belonging to edges
  public List<LXPoint> panelPoints; // Points belonging to panels
  public List<TEBox> boxes;
  public Boundaries boundaryPoints;

  // Boundaries are the points at the boundaries of our 3-dimensional grid. We retain
  // the `LXPoint` for convenience, but only the respective coordinate of each bound
  // point is important.
  public static class Boundaries {
    public final LXPoint minXBoundaryPoint;
    public final LXPoint maxXBoundaryPoint;
    public final LXPoint minYBoundaryPoint;
    public final LXPoint maxYBoundaryPoint;
    public final LXPoint minZBoundaryPoint;
    public final LXPoint maxZBoundaryPoint;

    public Boundaries(
      LXPoint minXBoundaryPoint,
      LXPoint maxXBoundaryPoint,
      LXPoint minYBoundaryPoint,
      LXPoint maxYBoundaryPoint,
      LXPoint minZBoundaryPoint,
      LXPoint maxZBoundaryPoint) {
      this.minXBoundaryPoint = minXBoundaryPoint;
      this.maxXBoundaryPoint = maxXBoundaryPoint;
      this.minYBoundaryPoint = minYBoundaryPoint;
      this.maxYBoundaryPoint = maxYBoundaryPoint;
      this.minZBoundaryPoint = minZBoundaryPoint;
      this.maxZBoundaryPoint = maxZBoundaryPoint;
    }
  }

  private static class Geometry {
    public String subdir;
    public String name;
    public LXPoint gapPoint;
    public HashMap<Integer, TEVertex> vertexesById;
    public HashMap<String, TEEdgeModel> edgesById;
    public HashMap<String, TEPanelModel> panelsById;
    public HashMap<TEPanelSection, Set<TEPanelModel>> panelsBySection;
    public HashMap<String, List<TEPanelModel>> panelsByFlavor;
    public HashMap<String, TELaserModel> lasersById;
    public List<TEBox> boxes;
    public LXModel[] children;
  }

  public TEWholeModel(String subdir) {
    this(loadGeometry(subdir));
  }

  private TEWholeModel(Geometry geometry) {
    super(geometry.children);
    this.subdir = geometry.subdir;
    this.name = geometry.name;
    this.gapPoint = geometry.gapPoint;
    this.vertexesById = geometry.vertexesById;
    this.edgePoints = new ArrayList<>();
    this.edgesById = geometry.edgesById;
    this.edgesBySymmetryGroup = new HashMap<>();
    buildEdgeRelations();

    this.panelsById = geometry.panelsById;
    this.panelsBySection = geometry.panelsBySection;
    this.panelsByFlavor = geometry.panelsByFlavor;

    this.panelPoints = new ArrayList<>();
    for (TEPanelModel p : this.panelsById.values()) {
      this.panelPoints.addAll(Arrays.asList(p.points));
    }

    this.lasersById = geometry.lasersById;
    this.boxes = geometry.boxes;

    reindexPoints();
    this.boundaryPoints = initializeBoundaries();
    LX.log(String.format("Min X boundary: %f", boundaryPoints.minXBoundaryPoint.x));
    LX.log(String.format("Max X boundary: %f", boundaryPoints.maxXBoundaryPoint.x));

    LX.log(String.format("Min Y boundary: %f", boundaryPoints.minYBoundaryPoint.y));
    LX.log(String.format("Max Y boundary: %f", boundaryPoints.maxYBoundaryPoint.y));

    LX.log(String.format("Min Z boundary: %f", boundaryPoints.minZBoundaryPoint.z));
    LX.log(String.format("Max Z boundary: %f", boundaryPoints.maxZBoundaryPoint.z));

    LX.log(this.name + " loaded. " +
           this.vertexesById.size() + " vertexes, " +
           this.edgesById.size() + " edges, " +
           this.panelsById.size() + " panels, " +
           this.points.length + " pixels");
  }

  public boolean isEdgePoint(int index) {
    return index >= edgePoints.get(0).index && index <= edgePoints.get(edgePoints.size()-1).index;
  }

  public boolean isPanelPoint(int index) {
    return index >= panelPoints.get(0).index && index <= panelPoints.get(panelPoints.size() - 1).index;
  }

  /** Builds structures that compute spacial relationships for edges,
   *  such as edges that are mirrored fore-aft and port-starboard.
   */
  private void buildEdgeRelations() {
    for (TEEdgeModel edge : this.edgesById.values()) {
      // In decimeters to better group
      int absY = Math.round(Math.abs(edge.center.y) / 100_000);
      int absZ = Math.round(Math.abs(edge.center.z) / 100_000);
      LXVector symmetryKey = new LXVector(0, absY * 100_000, absZ * 100_000);
      List<TEEdgeModel> symmetryGroup = this.edgesBySymmetryGroup
              .computeIfAbsent(symmetryKey, k -> new ArrayList<>());
      symmetryGroup.add(edge);
      edge.symmetryGroup = symmetryGroup;
      this.edgePoints.addAll(Arrays.asList(edge.points));
    }
  }

  private static Scanner loadFilePrivate(String filename) {
    try {
      File f = new File(filename);
      return new Scanner(f);
    } catch (FileNotFoundException e) {
      throw new Error(filename + " not found below " + System.getProperty("user.dir"));
    }
  }

  public Scanner loadFile(String filename) {
    return loadFilePrivate(this.subdir + "/" + filename);
  }

  private static void loadVertexes(Geometry geometry) {
    geometry.vertexesById = new HashMap<Integer, TEVertex>();
    Scanner s = loadFilePrivate(geometry.subdir + "/vertexes.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 4 : "Found " + tokens.length + " tokens";
      int id = Integer.parseInt(tokens[0]);
      int x = Integer.parseInt(tokens[1]);
      int y = Integer.parseInt(tokens[2]);
      int z = Integer.parseInt(tokens[3]);
      LXVector vector = new LXVector(x, y, z);
      TEVertex v = new TEVertex(vector, id);
      geometry.vertexesById.put(id, v);
    }
    s.close();
  }

  private static void registerController(TEModel subModel, String config, boolean fwd) {
    String[] tokens = config.split("#");
    assert tokens.length == 2;
    String ipAddress = tokens[0];
    tokens = tokens[1].split(":");
    assert tokens.length == 2;
    int universeNum = Integer.parseInt(tokens[0]);
    int strandOffset = Integer.parseInt(tokens[1]);
    // ipAddress = "127.0.0.1";
    TEArtNetOutput.registerSubmodel(subModel, ipAddress, universeNum, strandOffset, fwd);
  }

  private static void loadEdges(Geometry geometry) {
    geometry.edgesById = new HashMap<String, TEEdgeModel>();
    Scanner s = loadFilePrivate(geometry.subdir + "/edges.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 3 : "Found " + tokens.length + " tokens";

      String id = tokens[0];
      String edgeKind = tokens[1];
      String controller = tokens[2];

      boolean dark;
      boolean fwd = true;
      switch (edgeKind) {
        case "default":
          dark = false;
          break;
        case "reversed":
          dark = false;
          fwd = false;
          break;
        case "dark":
          dark = true;
          assert controller.equals("uncontrolled");
          break;
        default:
          throw new Error("Weird edge config: " + line);
      }

      tokens = id.split("-");
      if (tokens.length != 2) {
        throw new Error("Found " + tokens.length + " ID tokens");
      }
      int v0Id = Integer.parseInt(tokens[0]);
      int v1Id = Integer.parseInt(tokens[1]);
      assert v0Id < v1Id;
      TEVertex v0 = geometry.vertexesById.get(v0Id);
      TEVertex v1 = geometry.vertexesById.get(v1Id);
      TEEdgeModel e = new TEEdgeModel(v0, v1, dark);
      v0.addEdge(e);
      v1.addEdge(e);

      if (!controller.equals("uncontrolled")) {
        registerController(e, controller, fwd);
      }

      geometry.edgesById.put(id, e);
    }
    s.close();
  }

  private static int calcNudge(String s) {
    int rv = 0;
    for (char c : s.toCharArray()) {
      if (c == '-') rv--;
      else if (c == '+') rv++;
      else throw new IllegalArgumentException("Bad nudge char " + c);
    }
    return rv;
  }

  private static Map<String, Integer> loadPanelStartVertexes(Geometry geometry) {
    Scanner s = loadFilePrivate(geometry.subdir + "/panel_signal_paths.tsv");
    Map<String, Integer> rv = new HashMap<>();

    String headerLine = s.nextLine();
    assert headerLine.endsWith("Signal in vertex");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\\s+");
      assert tokens.length == 8;
      rv.put(tokens[0], Integer.parseInt(tokens[7]));
    }
    return rv;
  }

  private static Map<String, TEStripingInstructions> loadStripingInstructions(
          Geometry geometry, Map<String, Integer> startVertexes) {
    Scanner s = loadFilePrivate(geometry.subdir + "/striping-instructions.txt");

    Map<String, TEStripingInstructions> rv = new HashMap<>();
    while (s.hasNextLine()) {
      String line = s.nextLine()
              .replaceAll("\s*\\(.+?\\)\s*", " ");
      String[] tokens = line.split(" ");
      if (tokens[0].contains(".")) {
        LX.log("Ignoring leftover Striping IP " + tokens[0]);
        continue;
      }
      String id = tokens[0];
      if (tokens.length < 3) continue;
      int rowLength = Integer.parseInt(tokens[1]);

      int[] universeLengths = null;
      int next_index = 2;
      if (tokens[next_index].startsWith("U")) {
        universeLengths = Arrays.stream(tokens[next_index].substring(1).split(","))
                .mapToInt(Integer::parseInt).toArray();
        next_index++;
        // FIXME: Use these
      }
      boolean isLeft;
      if (tokens[next_index].equals("L")) {
        isLeft = true;
      } else if (tokens[next_index].equals("R")) {
        isLeft = false;
      } else {
        throw new Error("Invalid left/right token " + tokens[next_index]);
      }
      next_index++;
      List<Integer> rowLengths = new ArrayList<>();
      List<Integer> beforeNudges = new ArrayList<>();
      List<Integer> gaps = new ArrayList<>();
      int currentGap = 0;
      int phase = 0;
      for (int i = next_index; i < tokens.length; i++) {
        String token = tokens[i];
        if (token.matches("^g+$")) {
          currentGap += token.length();
        } else {
          String[] subTokens = token.split("\\.", -1);
          if (subTokens.length != 2) {
            throw new IllegalArgumentException("Bad subtokens for [" +
                    line + "]: " + token);
          }
          int leftNudge = calcNudge(subTokens[0]);
          int rightNudge = calcNudge(subTokens[1]);
          if (isLeft == (phase == 0)) {
            beforeNudges.add(leftNudge);
          } else {
            beforeNudges.add(rightNudge);
          }
          rowLength += leftNudge + rightNudge;
          rowLengths.add(rowLength);
          rowLength--;
          gaps.add(currentGap);
          currentGap = 0;
          phase = 1 - phase;
        }
      }
      int startingVertex = startVertexes.get(id);
      TEStripingInstructions tesi = new TEStripingInstructions(
              startingVertex, universeLengths,
              rowLengths.stream().mapToInt(i -> i).toArray(),
              beforeNudges.stream().mapToInt(i -> i).toArray(),
              gaps.stream().mapToInt(i -> i).toArray());
      rv.put(id, tesi);
    }
    return rv;
  }

  private static void loadPanels(Geometry geometry) {
    geometry.panelsById = new HashMap<>();
    geometry.panelsBySection = new HashMap<>();
    geometry.panelsByFlavor = new HashMap<>();

    Map<String, Integer> startVertexes = loadPanelStartVertexes(geometry);

    Map<String, TEStripingInstructions> stripingInstructions
            = loadStripingInstructions(geometry, startVertexes);

    for (String id : stripingInstructions.keySet()) {
      TEStripingInstructions tesi = stripingInstructions.get(id);
      StringBuilder out = new StringBuilder("Panel " + id +
              " has starting vertex " + tesi.startingVertex);
      if (tesi.universeLengths != null) {
        out.append(" and universe lengths ");
        for (int i : tesi.universeLengths) out.append(i).append(" ");
      }
      out.append(" and row lengths ");
      for (int i : tesi.rowLengths) out.append(i).append(" ");
      LX.log(out.toString());
    }

    Scanner s = loadFilePrivate(geometry.subdir + "/panels.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 6 : "Found " + tokens.length + " tokens";

      String id = tokens[0];
      String e0Id = tokens[1];
      String e1Id = tokens[2];
      String e2Id = tokens[3];
      String flipStr = tokens[4];
      String panelType = tokens[5];

      TEEdgeModel e0 = geometry.edgesById.get(e0Id);
      TEEdgeModel e1 = geometry.edgesById.get(e1Id);
      TEEdgeModel e2 = geometry.edgesById.get(e2Id);

      HashSet<TEVertex> vh = new HashSet<>();
      vh.add(e0.v0); vh.add(e0.v1);
      vh.add(e1.v0); vh.add(e1.v1);
      vh.add(e2.v0); vh.add(e2.v1);
      TEVertex[] vertexes = vh.toArray(new TEVertex[0]);
      assert vertexes.length == 3;

      boolean lit = panelType.contains(".");
      String outputConfig = panelType;

      if (lit) panelType = "lit";

      TEPanelModel p = TEPanelFactory.build(id, vertexes[0], vertexes[1], vertexes[2],
              e0, e1, e2, panelType, stripingInstructions.get(id), geometry.gapPoint);

      if (flipStr.equals("flipped")) {
        p.offsetTriangles.flip();
      } else if (!flipStr.equals("unflipped")) {
        throw new Error("Panel " + id + " is neither flipped nor unflipped");
      }

      e0.connectedPanels.add(p);
      e1.connectedPanels.add(p);
      e2.connectedPanels.add(p);

      geometry.panelsById.put(id, p);

      if (!geometry.panelsBySection.containsKey(p.getSection()))
        geometry.panelsBySection.put(p.getSection(), new HashSet<>());
      geometry.panelsBySection.get(p.getSection()).add(p);

      String flavor = p.flavor;
      if (!geometry.panelsByFlavor.containsKey(flavor))
        geometry.panelsByFlavor.put(flavor, new ArrayList<>());
      geometry.panelsByFlavor.get(flavor).add(p);

      // TODO: Do we need to support backwards-wired panels?
      if (lit) registerController(p, outputConfig, true);
    }
    s.close();

    for (String flavor : geometry.panelsByFlavor.keySet()) {
      StringBuilder flavorStr = new StringBuilder("Panels of flavor ");
      flavorStr.append(flavor);
      flavorStr.append(": ");
      for (TEPanelModel panel : geometry.panelsByFlavor.get(flavor)) {
        flavorStr.append(panel.id);
        flavorStr.append(" ");
      }
      // LX.log(flavorStr.toString());
    }
  }

  private static void loadLasers(Geometry geometry) {
    geometry.lasersById = new HashMap<>();

    Scanner s = loadFilePrivate(geometry.subdir + "/lasers.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 4 : "Found " + tokens.length + " tokens";

      String id = tokens[0];
      int x = Integer.parseInt(tokens[1]);
      int y = Integer.parseInt(tokens[2]);
      int z = Integer.parseInt(tokens[3]);

      TELaserModel laser = new TELaserModel(id, x, y, z);
      //laser.control = new Cone(laser);
      laser.control = new MovingTarget(laser);
      geometry.lasersById.put(id, laser);
    }
  }

  private static void loadBoxes(Geometry geometry) {
    geometry.boxes = new ArrayList<>();

    Scanner s = loadFilePrivate(geometry.subdir + "/boxes.txt");

    List<LXVector> vectors = new ArrayList<>();
    while (s.hasNextLine()) {
      String line = s.nextLine();
      if (line.isBlank()) continue;
      String[] tokens = line.split("\\s+");
      assert tokens.length == 3 : "Found " + tokens.length + " tokens";

      int x = Integer.parseInt(tokens[0]);
      int y = Integer.parseInt(tokens[1]);
      int z = Integer.parseInt(tokens[2]);

      vectors.add(new LXVector(x,y,z));

      if (vectors.size() == 8) {
        geometry.boxes.add(new TEBox(vectors));
        List<LXVector> mirrored = new ArrayList<>();
        for (LXVector v : vectors) {
          mirrored.add(new LXVector(v.x, v.y, -v.z));
        }
        geometry.boxes.add(new TEBox(mirrored));
        vectors.clear();
      }
    }
    assert vectors.size() == 0 : "Leftover lines in boxes.txt";
  }

  private static void loadGeneral(Geometry geometry) {
    Scanner s = loadFilePrivate(geometry.subdir + "/general.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split(":");
      assert tokens.length == 2 : "Found " + tokens.length + " tokens";
      switch (tokens[0].trim()) {
        case "name":
          geometry.name = tokens[1].trim();
          break;
        default:
          throw new Error("Weird line: " + line);
      }
    }
    s.close();
    assert geometry.name != null : "Model has no name";
  }

  // initializeBoundaries finds the boundaries of the grid we've drawn to contain our shape
  // and instantiates a helper class called Boundaries to keep track of the edge values
  // represented by that outermost LXPoint for each axis.
  private Boundaries initializeBoundaries() {
    ArrayList<LXPoint> pointsList = new ArrayList<>(Arrays.asList(this.points));

    LXPoint minXValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.x)).get();
    LXPoint maxXValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.x)).get();

    LXPoint minYValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.y)).get();
    LXPoint maxYValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.y)).get();

    LXPoint minZValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.z)).get();
    LXPoint maxZValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.z)).get();
    return new Boundaries(
      minXValuePoint,
      maxXValuePoint,
      minYValuePoint,
      maxYValuePoint,
      minZValuePoint,
      maxZValuePoint);
  }

  private static Geometry loadGeometry(String subdir) {
    Geometry geometry = new Geometry();
    geometry.subdir = "resources/" + subdir;
    List<LXModel> childList = new ArrayList<LXModel>();

    loadGeneral(geometry);

    loadBoxes(geometry);

    loadVertexes(geometry);

    // Vertexes aren't LXPoints (and thus, not LXModels) so they're not children

    loadLasers(geometry);

    childList.addAll(geometry.lasersById.values());

    loadEdges(geometry);

    childList.addAll(geometry.edgesById.values());

    geometry.gapPoint = new LXPoint();
    List<LXPoint> gapList = new ArrayList<>();
    gapList.add(geometry.gapPoint);
    childList.add(new LXModel(gapList));

    loadPanels(geometry);

    childList.addAll(geometry.panelsById.values());

    geometry.children = childList.toArray(new LXModel[0]);

    return geometry;
  }

  public Set<TEPanelModel> getPanelsBySection(TEPanelSection section) {
    return panelsBySection.get(section);
  }

  public Set<LXPoint> getPointsBySection(TEPanelSection section) {
    return getPanelsBySection(section)
            .stream()
            .map(LXModel::getPoints)
            .flatMap(List::stream)
            .collect(Collectors.toSet());
  }

  public Set<TEPanelModel> getPanelsBySections(Collection<TEPanelSection> sections) {
    return panelsBySection.entrySet().stream()
            .filter(entry -> sections.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
  }

  public Set<TEPanelModel> getLeftPanels() {
    return getPanelsBySections(List.of(TEPanelSection.STARBOARD_AFT,
            TEPanelSection.STARBOARD_AFT_SINGLE, TEPanelSection.AFT));
  }

  public Set<TEPanelModel> getRightPanels() {
    return getPanelsBySections(List.of(TEPanelSection.STARBOARD_FORE,
            TEPanelSection.STARBOARD_FORE_SINGLE, TEPanelSection.FORE));
  }

  public Set<TEPanelModel> getAllPanels() {
    return new HashSet<>(panelsById.values());
  }

  public Set<TEEdgeModel> getAllEdges() {
    return new HashSet<>(edgesById.values());
  }

}
