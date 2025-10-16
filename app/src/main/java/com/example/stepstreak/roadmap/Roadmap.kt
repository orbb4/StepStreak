package com.example.stepstreak.roadmap

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


var nodesRoadmap1: List<Node> = listOf(Node(), Node(), Node(NodeType.CHEST), Node(), Node(),  Node(NodeType.CHEST), Node(), Node(), Node(NodeType.CHEST), Node())
var edgesRoadmap1: List<Edge> = createEdgesFromNodes(nodesRoadmap1)
var roadmap1: Roadmap = Roadmap(nodesRoadmap1, edgesRoadmap1)
@Composable
fun DisplayRoadmap(roadmap: Roadmap, steps: Long?, onClick: () -> Unit){
    var offset_ = 20.dp
    Log.d("RoadmapDebug", "Total steps: $steps")

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth().padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        var stepcounter: Long = 5000
        for (node in roadmap.nodes) {
            if(steps!=null && steps >= stepcounter){
                node.isClaimable = true
            }else{
                node.isClaimable=false
            }
            Box(modifier = Modifier.offset(x = offset_)) {
                DisplayNode(node) { onClick() }
            }
            offset_*=-1
            stepcounter+=5000
        }
    }
}



@Composable
fun DisplayNode(node: Node, onClick: () -> Unit) {
    val canvasSize: Dp = if (node.type == NodeType.CHEST) 100.dp else 80.dp

    Box(
        modifier = Modifier
            .size(canvasSize)
            .clickable {
                if(node.isClaimable && !node.isClaimed){
                    node.claim()
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.matchParentSize()) {
            val color = if (node.isClaimable || node.isClaimed) Color.Blue.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.9f)
            val radius = size.minDimension / 2
            drawCircle(color = color, radius = radius)
            drawCircle(
                color = color.copy(alpha = 1f),
                radius = radius,
                style = Stroke(width = 20f)
            )
        }

        if (node.isClaimed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Claimable",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}




// A roadmap is a set of nodes and edges.
class Roadmap(val nodes: List<Node>, val edges: List<Edge>) {
    fun getCurrentNode(userSteps: Long): Node{
        var totalSteps: Long = 0
        for(edge in edges){
            totalSteps+=edge.stepsRequired
            if(userSteps < totalSteps){
                return edge.from
            }
        }
        return nodes.last()
    }
}
// A node is a point of a roadmap where the user is able to get rewards. The type of reward
// depends on the type of node (for more info check enum class NodeType)
class Node(val type: NodeType = NodeType.DEFAULT) {
    // did the user claim this node's items?
    var isClaimed by mutableStateOf(false)
    var isClaimable by mutableStateOf(false)

    fun claim() {
        isClaimed = true
    }

}


// An edge defines the distance between a pair of nodes. To travel from one node to another,
// the user must walk the amount of steps assigned to the edge that connects them
public data class Edge(val from: Node, val to:Node, val stepsRequired: Int)
fun createEdgesFromNodes(nodes: List<Node>): MutableList<Edge> {
    var edges = mutableListOf<Edge>()
    assert(nodes.size>1)
    var counter: Int = 0
    var prevNode: Node = nodes.first()
    for(node in nodes){
        if(counter==0){
            counter++
            continue
        }else{
            edges.add(Edge(prevNode, node, 3000))
        }
        counter++
    }
    return edges
}
enum class NodeType{
    DEFAULT, // user can claim fixed amount of coins from this node.
    CHEST // user can get random items from this node.
}