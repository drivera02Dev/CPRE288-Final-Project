import numpy as np
import math
import matplotlib.pyplot as plt
from matplotlib.patches import Polygon

class MapHandler:
    def __init__(self):
        self.map_size = 600
        self.map = np.zeros((self.map_size, self.map_size), dtype=np.uint8)
        self.position = [300, 300]  # Start in center
        self.direction = 90  # 0 = right, 90 = down
        self.shapes = []  # List of (polygon, color)
    
    def move_forward(self, centimeters):
        radians = math.radians(self.direction)
        dx = int(round(math.cos(radians) * centimeters))
        dy = int(round(math.sin(radians) * centimeters))
        self.position[0] += dx
        self.position[1] += dy
        self._clamp_position()
    
    def rotate(self, degrees):
        self.direction = (self.direction + degrees) % 360
    
    def add_obstacle(self, width, height, angle_from_current_direction, distance_from_current):
        angle = math.radians(self.direction + angle_from_current_direction)
        cx = self.position[0] + math.cos(angle) * distance_from_current
        cy = self.position[1] + math.sin(angle) * distance_from_current

        rect = self._rotated_rect(cx, cy, width, height, self.direction + angle_from_current_direction)
        self.shapes.append((rect, 'red'))
    
    def add_border(self):
        rect = self._rotated_rect(self.position[0], self.position[1], 10, 2, self.direction)
        self.shapes.append((rect, 'blue'))
    
    def _rotated_rect(self, cx, cy, w, h, angle_deg):
        angle = math.radians(angle_deg)
        dx = w / 2
        dy = h / 2
        corners = [
            (-dx, -dy),
            ( dx, -dy),
            ( dx,  dy),
            (-dx,  dy)
        ]
        rotated = []
        for x, y in corners:
            rx = x * math.cos(angle) - y * math.sin(angle)
            ry = x * math.sin(angle) + y * math.cos(angle)
            rotated.append((cx + rx, cy + ry))
        return np.array(rotated)  # Explicitly convert to NumPy array
    
    def draw_map(self):
        fig, ax = plt.subplots(figsize=(6, 6))
        ax.set_xlim(0, self.map_size)
        ax.set_ylim(0, self.map_size)
        ax.set_aspect('equal')
        ax.invert_yaxis()  # To match screen coords

        # Draw all shapes
        for polygon, color in self.shapes:
            poly_patch = Polygon(polygon, closed=True, color=color, alpha=0.5)
            ax.add_patch(poly_patch)

        # Draw current position
        ax.plot(self.position[0], self.position[1], 'go')  # green dot

        plt.title("Field Map")
        plt.grid(True)
        plt.show()
    
    def _clamp_position(self):
        self.position[0] = max(0, min(self.position[0], self.map_size - 1))
        self.position[1] = max(0, min(self.position[1], self.map_size - 1))


# Example usage
handler = MapHandler()
handler.move_forward(50)
handler.rotate(-90)
handler.move_forward(50)
handler.add_obstacle(width=10, height=5, angle_from_current_direction=45, distance_from_current=20)
handler.add_border()
handler.rotate(45)
handler.move_forward(40)
handler.draw_map()
