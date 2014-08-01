/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.cmd.NoDataHandler

import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.FileSplit

import java.awt.image._

class GeotiffInputFormat extends FileInputFormat[Extent, Tile] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Extent, Tile] =
    new GeotiffRecordReader

}

case class GeotiffData(cellType: CellType, userNoData: Double)

class GeotiffRecordReader extends RecordReader[Extent, Tile] {
  private var tup: (Extent, Tile) = null
  private var hasNext: Boolean = true

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val file = split.asInstanceOf[FileSplit].getPath()
    tup =
      GeoTiff.withReader[(Extent, Tile)](file, context.getConfiguration()) { reader =>
        val image = GeoTiff.getGridCoverage2D(reader)
        val imageMeta = GeoTiff.getMetadata(reader)
        val (cols, rows) = imageMeta.pixels
        val rawDataBuff = image.getRenderedImage().getData().getDataBuffer()
        val cellType = CellType.fromAwtType(imageMeta.cellType)
        
        val tile =  cellType match {
          case TypeDouble => ArrayTile(rawDataBuff.asInstanceOf[DataBufferDouble].getData(), cols, rows)
          case TypeFloat  => ArrayTile(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), cols, rows)
          case TypeInt    => ArrayTile(rawDataBuff.asInstanceOf[DataBufferInt].getData(), cols, rows)
          case TypeShort  => ArrayTile(rawDataBuff.asInstanceOf[DataBufferShort].getData(), cols, rows)
          case TypeByte   => ArrayTile(rawDataBuff.asInstanceOf[DataBufferByte].getData(), cols, rows)
          case TypeBit   => sys.error("TypeBit is not yet supported")
        }
        NoDataHandler.removeUserNoData(tile, imageMeta.nodata)
        (imageMeta.extent, tile)
      }
  }

  def close = {}
  def getCurrentKey = tup._1
  def getCurrentValue = { hasNext = false ; tup._2 }
  def getProgress = 1
  def nextKeyValue = hasNext
}
