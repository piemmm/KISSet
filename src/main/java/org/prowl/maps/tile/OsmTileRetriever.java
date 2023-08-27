/*
 * Copyright (c) 2018, 2020, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.prowl.maps.tile;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * This is the OSM tile renderer, part of gluon maps, bue ripped out because I need to use a custom tile server
 * in a java18 project without the ballache of the service loader, when instead a simple setTileRetriever(...) would
 * have done for 99% of users use-cases out there.
 */
public class OsmTileRetriever implements TileRetriever {

    private Semaphore imageLimit = new Semaphore(2);

    private static final Log LOG = LogFactory.getLog("OsmTileRetriever");
    private static final String host = "https://tile.openstreetmap.org/";
    static final String httpAgent;

    static {
        String agent = System.getProperty("http.agent");
        if (agent == null) {
            agent = "(" + System.getProperty("os.name") + " / " + System.getProperty("os.version") + " / " + System.getProperty("os.arch") + ")";
        }
        httpAgent = "KISSet/0.0.1 " + agent;
        System.setProperty("http.agent", httpAgent);
    }

    static String buildImageUrlString(int zoom, long i, long j) {
        return host + zoom + "/" + i + "/" + j + ".png";
    }

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long i, long j) {

        // Image is cached!
        File cacheFile = new File(KISSet.INSTANCE.getStorage().getMapStorageDir(), zoom + "-" + i + "-" + j + ".png");
        if (cacheFile.exists()) {
            return CompletableFuture.completedFuture(new Image(cacheFile.toURI().toString(), false));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                imageLimit.acquire();
                String urlString = buildImageUrlString(zoom, i, j);
                Image image = new Image(urlString, true);
                image.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (image.isError()) {
                        LOG.error("Error loading image:" + image.getException().getMessage(), image.getException());
                    } else if (newValue.intValue() == 1) {
                        try {
                            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", cacheFile);
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }

                });

                return image;
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                imageLimit.release();
            }

            return null;
        });
    }
}
