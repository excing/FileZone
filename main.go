package main

import (
	"fmt"
	"os"
	"time"
	"runtime"
	"flag"
	"net/http"
	"io"
	"path/filepath"
	"regexp"
	"strconv"
)

// go tool pprof http://localhost:6060/debug/pprof/heap
// @see https://golang.org/pkg/net/http/pprof/
// import _ "net/http/pprof"

var StorageRootPath string

var FileServerPort int

func downloadFileHandle(w http.ResponseWriter, r *http.Request, filename string) {
  flPath := filepath.Join(StorageRootPath, filename)
  if _, err := os.Stat(flPath); os.IsNotExist(err) {
		fmt.Println(err)
		http.NotFound(w, r)
		return
	}

	content, err := os.Open(flPath)
	if err != nil {
		fmt.Println(err)
		http.NotFound(w, r)
		return
	}
	defer content.Close()

	fmt.Println("download finish:", flPath)
	http.ServeContent(w, r, filename, time.Time{}, content)
}

// 上传图像接口
func uploadFileHandle(w http.ResponseWriter, r *http.Request) {
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

	wirteResponse(w, "{\"code\": 0}")
	fmt.Println("save finish:", FilePath)
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

var validPath = regexp.MustCompile("^/(assets)/(.+)$")

func makeHandler(fn func(http.ResponseWriter, *http.Request, string)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		fmt.Println(r.URL.Path)
		m := validPath.FindStringSubmatch(r.URL.Path)
		if m == nil {
			http.NotFound(w, r)
			return
		}
		fn(w, r, m[2])
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
	flag.IntVar(&FileServerPort, "p", 8090, "file server port")
}

func main() {
	flag.Parse()
	flag.Usage()

	StorageRootPath, _ = filepath.Abs(StorageRootPath)

	var err error
	if _, err = os.Stat(StorageRootPath); os.IsNotExist(err) {
		err = os.MkdirAll(StorageRootPath, os.ModePerm)
		if err != nil {
			fmt.Println("Can't create storage root directory")
			return
		}
	}

	fmt.Println("args:", StorageRootPath, "," , strconv.Itoa(FileServerPort))

	http.HandleFunc("/upload", uploadFileHandle)
	http.HandleFunc("/assets/", makeHandler(downloadFileHandle))
	err = http.ListenAndServe("0.0.0.0:" + strconv.Itoa(FileServerPort), nil)
	fmt.Println(err)
}
