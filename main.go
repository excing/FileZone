package main

import (
	"fmt"
	"os"
	"runtime"
	"flag"
	"net/http"
	"io"
	"path/filepath"
)

var StorageRootPath string

var FileServerPort string

func downloadFileHandle(w http.ResponseWriter, r *http.Request) {
	fmt.Println(r.URL)
	http.ServeFile(w, r, r.URL.Path[1:])
}

// 上传图像接口
func uploadFileHandle(w http.ResponseWriter, r *http.Request) {
	os.Mkdir(StorageRootPath, os.ModePerm)

	file, handler, err := r.FormFile("file")
	if err != nil {
		fmt.Println(err)
		errorHandle(err, w)
		return
	}
	defer file.Close()
	fmt.Println(handler.Header)

  flPath := filepath.Join(StorageRootPath, handler.Filename)
	f1, err := os.OpenFile(flPath, os.O_WRONLY|os.O_CREATE, 0666)
	if err != nil {
		fmt.Println(err)
		errorHandle(err, w)
		return
	}
	defer f1.Close()
	io.Copy(f1, file)

	FilePath, _ := filepath.Abs(f1.Name())

	fmt.Println("file path:", FilePath)
	wirteResponse(w, "{\"code\": 0}")
}

func wirteResponse(w http.ResponseWriter, body string) {
	w.Header().Set("Access-Control-Allow-Origin", "*")             //允许访问所有域
	w.Header().Add("Access-Control-Allow-Headers", "Content-Type") //header的类型
	w.Header().Add("Content-Type", "Application/json")             //header的类型
	w.Write([]byte(body))
}

// 统一错误输出接口
func errorHandle(err error, w http.ResponseWriter) {
	if err != nil {
		w.Write([]byte(err.Error()))
	}
}

func SetStorageRootPath(Path string) {
	StorageRootPath = Path
}

func init() {
	switch runtime.GOOS {
		case "windows":
			flag.StringVar(&StorageRootPath, "s", "assets", "file storage root path")
		case "darwin":
			flag.StringVar(&StorageRootPath, "s", "assets", "file storage root path")
		case "linux":
			flag.StringVar(&StorageRootPath, "s", "/var/www/assets", "file storage root path")
	}
	flag.StringVar(&FileServerPort, "p", "8090", "file server port")
}

func main() {
	flag.Parse()
	flag.Usage()

	StorageRootPath, _ = filepath.Abs(StorageRootPath)

	fmt.Println("args:", StorageRootPath, "," , FileServerPort)

	http.HandleFunc("/upload", uploadFileHandle)
	http.HandleFunc("/assets/", downloadFileHandle)
	err := http.ListenAndServe(":" + FileServerPort, nil)
	fmt.Println(err)
}
