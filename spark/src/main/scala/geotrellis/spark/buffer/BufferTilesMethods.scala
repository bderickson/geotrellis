/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.buffer

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class BufferTilesMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
](val self: RDD[(K, V)]) extends MethodExtensions[RDD[(K, V)]] {
  def bufferTiles(bufferSize: Int): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSize)

  def bufferTiles(bufferSize: Int, layerBounds: GridBounds): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSize, layerBounds)

  @deprecated("Please specify buffer sizes per key as a function K => BufferSizes", "1.2")
  def bufferTiles(bufferSizesPerKey: RDD[(K, BufferSizes)]): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSizesPerKey)

  def bufferTiles(getBufferSizes: K => BufferSizes): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, getBufferSizes)
}
